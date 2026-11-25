package finalproject.vegastripplanner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/**
 * The ONE place the app opens the database.
 * Every DAO / controller should call Database.connect() instead of DriverManager directly.
 *
 * Call Database.initialize() once when the app starts (before showing the first window).
 * It makes sure the database file exists and has every table the app needs.
 */
public final class Database {

    /** Where the live database file lives. Relative = the folder the app is run from
     *  (in IntelliJ that's the project root, next to pom.xml). */
    private static final Path DB_FILE = Path.of("AllInVegas.db");

    /** Optional starter copy inside src/main/resources/finalproject/vegastripplanner/.
     *  Used only if DB_FILE doesn't exist yet (e.g. a fresh clone). */
    private static final String STARTER_RESOURCE = "/finalproject/vegastripplanner/AllInVegas.db";

    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    /** Bump this and add a new "if (version < N)" block below whenever the schema changes. */
    private static final int LATEST_VERSION = 3;

    private Database() {}

    static {
        try {
            Class.forName("org.sqlite.JDBC");   // makes sure the SQLite driver is loaded
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /** Opens a connection with foreign keys ON (SQLite turns them off by default). */
    public static Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    /** Run once at startup. */
    public static void initialize() {
        try {
            ensureFileExists();
            migrate();
        } catch (Exception e) {
            throw new RuntimeException("Could not set up the database at "
                    + DB_FILE.toAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------

    private static void ensureFileExists() throws IOException {
        if (Files.exists(DB_FILE)) return;
        try (InputStream in = Database.class.getResourceAsStream(STARTER_RESOURCE)) {
            if (in == null) {
                throw new IOException("AllInVegas.db not found next to pom.xml and no starter copy in resources");
            }
            Files.copy(in, DB_FILE);
        }
    }

    /**
     * Brings any copy of the database up to date.
     * SQLite has a built-in number slot, "user_version" (starts at 0), that we use to
     * remember which changes have already been applied to this file.
     */
    private static void migrate() throws SQLException {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            int version;
            try (ResultSet rs = st.executeQuery("PRAGMA user_version")) {
                version = rs.getInt(1);
            }
            if (version >= LATEST_VERSION) return;

            c.setAutoCommit(false);
            try {
                if (version < 1) {
                    for (String sql : VERSION_1) st.execute(sql);
                }
                if (version < 2) {
                    // The create-account page asks for a birthday
                    addColumnIfMissing(c, "usersTable", "birthday", "TEXT");
                }
                if (version < 3) {
                    // The Account settings page
                    addColumnIfMissing(c, "usersTable", "time_zone", "TEXT");
                    addColumnIfMissing(c, "usersTable", "distance_units", "TEXT");
                    addColumnIfMissing(c, "usersTable", "password_changed_at", "TEXT");
                }
                // Next change goes here, e.g.:
                // if (version < 4) {
                //     addColumnIfMissing(c, "placesTable", "description", "TEXT");
                // }
                st.execute("PRAGMA user_version = " + LATEST_VERSION);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Adds a column only if the table doesn't already have it (safe on any copy). */
    private static void addColumnIfMissing(Connection c, String table, String column, String type)
            throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (rs.getString("name").equalsIgnoreCase(column)) return;
            }
        }
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }

    /** Version 1: accounts, preferences, interests, favorites, itineraries.
     *  "IF NOT EXISTS" means this is harmless on a copy that already has the tables. */
    private static final String[] VERSION_1 = {
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_places_place_id ON placesTable(place_ID)",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_category_category_id ON categoryTable(category_ID)",

            """
        CREATE TABLE IF NOT EXISTS usersTable (
            user_ID        INTEGER PRIMARY KEY AUTOINCREMENT,
            first_name     TEXT NOT NULL,
            last_name      TEXT NOT NULL,
            username       TEXT NOT NULL UNIQUE COLLATE NOCASE,
            password_hash  TEXT NOT NULL,
            password_salt  TEXT NOT NULL,
            email          TEXT UNIQUE COLLATE NOCASE,
            phone          TEXT,
            created_at     TEXT NOT NULL DEFAULT (datetime('now')),
            CHECK (email IS NOT NULL OR phone IS NOT NULL)
        )""",

            """
        CREATE TABLE IF NOT EXISTS userPreferencesTable (
            user_ID          INTEGER PRIMARY KEY REFERENCES usersTable(user_ID) ON DELETE CASCADE,
            daily_budget_ID  INTEGER REFERENCES priceTable(price_ID),
            adults           INTEGER NOT NULL DEFAULT 1,
            children         INTEGER NOT NULL DEFAULT 0,
            arrival_date     TEXT,
            departure_date   TEXT
        )""",

            """
        CREATE TABLE IF NOT EXISTS userInterestsTable (
            user_ID      INTEGER NOT NULL REFERENCES usersTable(user_ID) ON DELETE CASCADE,
            category_ID  INTEGER NOT NULL REFERENCES categoryTable(category_ID),
            PRIMARY KEY (user_ID, category_ID)
        )""",

            """
        CREATE TABLE IF NOT EXISTS favoritesTable (
            user_ID   INTEGER NOT NULL REFERENCES usersTable(user_ID) ON DELETE CASCADE,
            place_ID  INTEGER NOT NULL REFERENCES placesTable(place_ID) ON DELETE CASCADE,
            added_at  TEXT DEFAULT (datetime('now')),
            PRIMARY KEY (user_ID, place_ID)
        )""",

            """
        CREATE TABLE IF NOT EXISTS itinerariesTable (
            itinerary_ID  INTEGER PRIMARY KEY AUTOINCREMENT,
            user_ID       INTEGER NOT NULL REFERENCES usersTable(user_ID) ON DELETE CASCADE,
            name          TEXT NOT NULL,
            start_date    TEXT,
            end_date      TEXT,
            created_at    TEXT DEFAULT (datetime('now'))
        )""",

            """
        CREATE TABLE IF NOT EXISTS itineraryItemsTable (
            item_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
            itinerary_ID  INTEGER NOT NULL REFERENCES itinerariesTable(itinerary_ID) ON DELETE CASCADE,
            place_ID      INTEGER NOT NULL REFERENCES placesTable(place_ID) ON DELETE CASCADE,
            day_number    INTEGER NOT NULL DEFAULT 1,
            sort_order    INTEGER NOT NULL DEFAULT 0
        )""",

            "CREATE INDEX IF NOT EXISTS idx_itineraries_user ON itinerariesTable(user_ID)",
            "CREATE INDEX IF NOT EXISTS idx_items_itinerary ON itineraryItemsTable(itinerary_ID, day_number, sort_order)"
    };
}