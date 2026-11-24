package finalproject.vegastripplanner;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Wording and day-counting for trips, shared by the dashboard and the itinerary page. */
public final class Trips {

    /** Trips without dates can still spread over this many days. */
    public static final int MAX_DAYS_WITHOUT_DATES = 14;

    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter MONTH_DAY_YEAR = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EEE, MMM d");
    private static final DateTimeFormatter LONG_WEEKDAY = DateTimeFormatter.ofPattern("EEEE");

    private Trips() {}

    /** UPCOMING, HAPPENING NOW, PAST TRIP or DRAFT (no dates yet). */
    public static String status(UserDAO.Itinerary trip) {
        if (trip.startDate() == null) return "DRAFT";
        LocalDate today = LocalDate.now();
        LocalDate end = trip.endDate() != null ? trip.endDate() : trip.startDate();
        if (today.isBefore(trip.startDate())) return "UPCOMING";
        if (!today.isAfter(end)) return "HAPPENING NOW";
        return "PAST TRIP";
    }

    /** True for trips that haven't finished yet (used for "Next escape"). */
    public static boolean isUpcomingOrNow(UserDAO.Itinerary trip) {
        String status = status(trip);
        return status.equals("UPCOMING") || status.equals("HAPPENING NOW");
    }

    /** "Dec 11–13, 2026", "Dec 30, 2026 – Jan 2, 2027", or "No dates yet". */
    public static String dateRange(UserDAO.Itinerary trip) {
        LocalDate start = trip.startDate();
        LocalDate end = trip.endDate();
        if (start == null) return "No dates yet";
        if (end == null || end.equals(start)) return start.format(MONTH_DAY_YEAR);
        if (start.getYear() != end.getYear()) return start.format(MONTH_DAY_YEAR) + " – " + end.format(MONTH_DAY_YEAR);
        if (start.getMonth() == end.getMonth()) {
            return start.format(MONTH_DAY) + "–" + end.getDayOfMonth() + ", " + end.getYear();
        }
        return start.format(MONTH_DAY) + " – " + end.format(MONTH_DAY) + ", " + end.getYear();
    }

    /** "Dec 11–13" (no year), for small spaces. */
    public static String shortRange(UserDAO.Itinerary trip) {
        LocalDate start = trip.startDate();
        LocalDate end = trip.endDate();
        if (start == null) return "No dates yet";
        if (end == null || end.equals(start)) return start.format(MONTH_DAY);
        if (start.getMonth() == end.getMonth()) return start.format(MONTH_DAY) + "–" + end.getDayOfMonth();
        return start.format(MONTH_DAY) + " – " + end.format(MONTH_DAY);
    }

    /** Trips with dates have one day per date; trips without dates go up to the last day used. */
    public static int dayCount(UserDAO.Itinerary trip, List<UserDAO.ItineraryItem> items) {
        if (trip.startDate() != null && trip.endDate() != null) return maxDays(trip);
        int lastUsed = 1;
        for (UserDAO.ItineraryItem item : items) lastUsed = Math.max(lastUsed, item.dayNumber());
        return lastUsed;
    }

    /** The highest day number a stop can be moved to. */
    public static int maxDays(UserDAO.Itinerary trip) {
        if (trip.startDate() != null && trip.endDate() != null) {
            return Math.max(1, (int) ChronoUnit.DAYS.between(trip.startDate(), trip.endDate()) + 1);
        }
        return MAX_DAYS_WITHOUT_DATES;
    }

    /** The date of a day in the trip, or null if the trip has no start date. */
    public static LocalDate dateOf(UserDAO.Itinerary trip, int day) {
        return trip.startDate() == null ? null : trip.startDate().plusDays(day - 1);
    }

    /** Day tab text: "Fri, Dec 11", or "Day 2" without dates. */
    public static String dayTab(UserDAO.Itinerary trip, int day) {
        LocalDate date = dateOf(trip, day);
        return date == null ? "Day " + day : date.format(WEEKDAY);
    }

    /** Day heading: "Friday on the Strip", or "Day 2 on the Strip" without dates. */
    public static String dayTitle(UserDAO.Itinerary trip, int day) {
        LocalDate date = dateOf(trip, day);
        return (date == null ? "Day " + day : date.format(LONG_WEEKDAY)) + " on the Strip";
    }

    /** Adults + children from the user's saved preferences (0 if none saved). */
    public static int travelers(int userId) {
        try {
            UserDAO.Preferences prefs = UserDAO.getPreferences(userId);
            return prefs == null ? 0 : prefs.adults() + prefs.children();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /** plural(1, "place") = "1 place", plural(3, "place") = "3 places". */
    public static String plural(int count, String word) {
        return count + " " + word + (count == 1 ? "" : "s");
    }
}