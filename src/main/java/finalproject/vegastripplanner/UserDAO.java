package finalproject.vegastripplanner;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * All database work for accounts, preferences, favorites and itineraries.
 * Connections come from Database.connect().
 */
public class UserDAO {

    // ------------------------------------------------------------------
    // Data holders returned to controllers
    // ------------------------------------------------------------------
    public record User(int id, String firstName, String lastName, String username,
                       String email, String phone) {}

    public record Preferences(Integer budgetPriceId, int adults, int children,
                              LocalDate arrival, LocalDate departure, List<Integer> interestCategoryIds) {}

    public record FavoritePlace(int placeId, String placeName, String category) {}

    public record Itinerary(int id, String name, LocalDate startDate, LocalDate endDate) {}

    public record ItineraryItem(int itemId, int placeId, String placeName,
                                int dayNumber, int sortOrder) {}

    // ==================================================================
    // ACCOUNTS
    // ==================================================================

    /** Creates an account and returns the new user_ID.
     *  Throws SQLException mentioning "usersTable.username" or "usersTable.email"
     *  if the username or email is already taken. */
    public static int createAccount(String first, String last, String username, String password,
                                    String email, String phone, LocalDate birthday) throws SQLException {
        String salt = newSalt();
        String hash = hashPassword(password, salt);
        String sql = """
            INSERT INTO usersTable (first_name, last_name, username, password_hash, password_salt, email, phone, birthday)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, first.trim());
            ps.setString(2, last.trim());
            ps.setString(3, username.trim());
            ps.setString(4, hash);
            ps.setString(5, salt);
            ps.setString(6, blankToNull(email));
            ps.setString(7, blankToNull(phone));
            ps.setString(8, dateToText(birthday));
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    public static boolean isUsernameAvailable(String username) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM usersTable WHERE username = ?")) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        }
    }

    /** Returns the User if username + password match, otherwise null. */
    public static User login(String username, String password) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM usersTable WHERE username = ?")) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String expected = rs.getString("password_hash");
                String actual = hashPassword(password, rs.getString("password_salt"));
                if (!MessageDigest.isEqual(expected.getBytes(), actual.getBytes())) return null;
                return new User(rs.getInt("user_ID"), rs.getString("first_name"),
                        rs.getString("last_name"), rs.getString("username"),
                        rs.getString("email"), rs.getString("phone"));
            }
        }
    }

    // ==================================================================
    // PREFERENCES
    // ==================================================================

    /** Saves (or replaces) a user's preferences and interest picks.
     *  budgetPriceId: price_ID from priceTable (1 Free, 2 $, 3 $$, 4 $$$) or null.
     *  interestCategoryIds: category_IDs (1 restaurant, 2 bar, 3 hotel, 4 entertainment,
     *  5 shopping, 6 casino, 7 attraction). */
    public static void savePreferences(int userId, Integer budgetPriceId, int adults, int children,
                                       LocalDate arrival, LocalDate departure,
                                       List<Integer> interestCategoryIds) throws SQLException {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                String upsert = """
                    INSERT INTO userPreferencesTable (user_ID, daily_budget_ID, adults, children, arrival_date, departure_date)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(user_ID) DO UPDATE SET
                        daily_budget_ID = excluded.daily_budget_ID,
                        adults          = excluded.adults,
                        children        = excluded.children,
                        arrival_date    = excluded.arrival_date,
                        departure_date  = excluded.departure_date""";
                try (PreparedStatement ps = c.prepareStatement(upsert)) {
                    ps.setInt(1, userId);
                    if (budgetPriceId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, budgetPriceId);
                    ps.setInt(3, adults);
                    ps.setInt(4, children);
                    ps.setString(5, dateToText(arrival));
                    ps.setString(6, dateToText(departure));
                    ps.executeUpdate();
                }
                try (PreparedStatement del = c.prepareStatement("DELETE FROM userInterestsTable WHERE user_ID = ?")) {
                    del.setInt(1, userId);
                    del.executeUpdate();
                }
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO userInterestsTable (user_ID, category_ID) VALUES (?, ?)")) {
                    for (int catId : interestCategoryIds) {
                        ins.setInt(1, userId);
                        ins.setInt(2, catId);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Loads saved preferences to pre-fill the Planning Trip page, or null if none saved yet. */
    public static Preferences getPreferences(int userId) throws SQLException {
        try (Connection c = Database.connect()) {
            List<Integer> interests = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT category_ID FROM userInterestsTable WHERE user_ID = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) interests.add(rs.getInt(1));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * FROM userPreferencesTable WHERE user_ID = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    int budget = rs.getInt("daily_budget_ID");
                    Integer budgetId = rs.wasNull() ? null : budget;
                    return new Preferences(budgetId, rs.getInt("adults"), rs.getInt("children"),
                            textToDate(rs.getString("arrival_date")),
                            textToDate(rs.getString("departure_date")), interests);
                }
            }
        }
    }

    // ==================================================================
    // FAVORITES
    // ==================================================================

    public static void addFavorite(int userId, int placeId) throws SQLException {
        runUpdate("INSERT OR IGNORE INTO favoritesTable (user_ID, place_ID) VALUES (?, ?)", userId, placeId);
    }

    public static void removeFavorite(int userId, int placeId) throws SQLException {
        runUpdate("DELETE FROM favoritesTable WHERE user_ID = ? AND place_ID = ?", userId, placeId);
    }

    public static boolean isFavorite(int userId, int placeId) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM favoritesTable WHERE user_ID = ? AND place_ID = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, placeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** The user's favorites with name + category, newest first (ready for a favorites list). */
    public static List<FavoritePlace> getFavorites(int userId) throws SQLException {
        String sql = """
            SELECT p.place_ID, p.place_name, cat.category
            FROM favoritesTable f
            JOIN placesTable p        ON p.place_ID = f.place_ID
            LEFT JOIN categoryTable cat ON cat.category_ID = p.category_ID
            WHERE f.user_ID = ?
            ORDER BY f.added_at DESC""";
        List<FavoritePlace> list = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new FavoritePlace(rs.getInt("place_ID"), rs.getString("place_name"),
                            rs.getString("category")));
                }
            }
        }
        return list;
    }

    // ==================================================================
    // ITINERARIES
    // ==================================================================

    public static int createItinerary(int userId, String name, LocalDate start, LocalDate end) throws SQLException {
        String sql = "INSERT INTO itinerariesTable (user_ID, name, start_date, end_date) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, (name == null || name.isBlank()) ? "My Vegas Trip" : name.trim());
            ps.setString(3, dateToText(start));
            ps.setString(4, dateToText(end));
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    public static List<Itinerary> getItineraries(int userId) throws SQLException {
        List<Itinerary> list = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM itinerariesTable WHERE user_ID = ? ORDER BY itinerary_ID DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Itinerary(rs.getInt("itinerary_ID"), rs.getString("name"),
                            textToDate(rs.getString("start_date")), textToDate(rs.getString("end_date"))));
                }
            }
        }
        return list;
    }

    public static void renameItinerary(int itineraryId, String newName) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE itinerariesTable SET name = ? WHERE itinerary_ID = ?")) {
            ps.setString(1, newName.trim());
            ps.setInt(2, itineraryId);
            ps.executeUpdate();
        }
    }

    /** Deletes a trip; its stops are removed automatically (ON DELETE CASCADE). */
    public static void deleteItinerary(int itineraryId) throws SQLException {
        runUpdate("DELETE FROM itinerariesTable WHERE itinerary_ID = ?", itineraryId);
    }

    /** Adds a place to the end of the given day. Returns the new item_ID. */
    public static int addPlaceToItinerary(int itineraryId, int placeId, int dayNumber) throws SQLException {
        String sql = """
            INSERT INTO itineraryItemsTable (itinerary_ID, place_ID, day_number, sort_order)
            VALUES (?, ?, ?,
                    (SELECT COALESCE(MAX(sort_order) + 1, 0) FROM itineraryItemsTable
                     WHERE itinerary_ID = ? AND day_number = ?))""";
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, itineraryId);
            ps.setInt(2, placeId);
            ps.setInt(3, dayNumber);
            ps.setInt(4, itineraryId);
            ps.setInt(5, dayNumber);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    /** Moves a stop to another day, placing it at the end of that day. */
    public static void moveItemToDay(int itemId, int newDay) throws SQLException {
        String sql = """
            UPDATE itineraryItemsTable
            SET day_number = ?,
                sort_order = (SELECT COALESCE(MAX(sort_order) + 1, 0) FROM itineraryItemsTable
                              WHERE itinerary_ID = (SELECT itinerary_ID FROM itineraryItemsTable WHERE item_ID = ?)
                                AND day_number = ?)
            WHERE item_ID = ?""";
        runUpdate(sql, newDay, itemId, newDay, itemId);
    }

    public static void removeItineraryItem(int itemId) throws SQLException {
        runUpdate("DELETE FROM itineraryItemsTable WHERE item_ID = ?", itemId);
    }

    /** Removes every stop for this place from the trip (used by the "Saved to my trip" toggle). */
    public static void removePlaceFromItinerary(int itineraryId, int placeId) throws SQLException {
        runUpdate("DELETE FROM itineraryItemsTable WHERE itinerary_ID = ? AND place_ID = ?", itineraryId, placeId);
    }

    /** IDs of the places already in a trip (to show "✓ Saved to my trip"). */
    public static Set<Integer> getPlaceIdsInItinerary(int itineraryId) throws SQLException {
        Set<Integer> ids = new HashSet<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT place_ID FROM itineraryItemsTable WHERE itinerary_ID = ?")) {
            ps.setInt(1, itineraryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    /**
     * The trip "Save to my trip" should add to:
     * the one picked this session, otherwise the user's newest trip,
     * otherwise a brand-new "My Vegas Trip". Remembers the answer in Session.
     */
    public static Itinerary getOrCreateCurrentItinerary(int userId) throws SQLException {
        List<Itinerary> trips = getItineraries(userId);   // newest first
        Integer currentId = Session.getCurrentItineraryId();
        if (currentId != null) {
            for (Itinerary t : trips) {
                if (t.id() == currentId) return t;
            }
        }
        Itinerary trip;
        if (!trips.isEmpty()) {
            trip = trips.get(0);
        } else {
            int newId = createItinerary(userId, "My Vegas Trip", null, null);
            trip = new Itinerary(newId, "My Vegas Trip", null, null);
        }
        Session.setCurrentItineraryId(trip.id());
        return trip;
    }

    /** All stops in a trip, day 1 first, then by position, with place names. */
    public static List<ItineraryItem> getItineraryItems(int itineraryId) throws SQLException {
        String sql = """
            SELECT i.item_ID, i.place_ID, p.place_name, i.day_number, i.sort_order
            FROM itineraryItemsTable i
            JOIN placesTable p ON p.place_ID = i.place_ID
            WHERE i.itinerary_ID = ?
            ORDER BY i.day_number, i.sort_order""";
        List<ItineraryItem> list = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itineraryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ItineraryItem(rs.getInt("item_ID"), rs.getInt("place_ID"),
                            rs.getString("place_name"), rs.getInt("day_number"), rs.getInt("sort_order")));
                }
            }
        }
        return list;
    }

    // ==================================================================
    // helpers
    // ==================================================================

    private static void runUpdate(String sql, int... params) throws SQLException {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setInt(i + 1, params[i]);
            ps.executeUpdate();
        }
    }

    private static int generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            keys.next();
            return keys.getInt(1);
        }
    }

    /** LocalDate -> "YYYY-MM-DD" (or null). */
    private static String dateToText(LocalDate d) {
        return d == null ? null : d.toString();
    }

    /** "YYYY-MM-DD" -> LocalDate (or null). */
    private static LocalDate textToDate(String s) {
        return (s == null || s.isBlank()) ? null : LocalDate.parse(s);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String newSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private static String hashPassword(String password, String saltBase64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 210_000, 256);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
}