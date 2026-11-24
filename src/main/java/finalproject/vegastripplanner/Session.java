package finalproject.vegastripplanner;

/**
 * Remembers who is logged in (and which trip they're working on),
 * so every page/controller can get to it.
 *
 *   Session.login(user);                    // after UserDAO.login(...) succeeds
 *   int id = Session.getUser().id();        // on any page
 *   if (!Session.isLoggedIn()) { ... }      // guest browsing via "Skip for now" / "Explore"
 *   Session.logout();
 */
public final class Session {

    private static UserDAO.User currentUser;

    // The trip that "Save to my trip" adds places to. null = not picked yet
    // (UserDAO.getOrCreateCurrentItinerary then uses the newest trip, or makes one).
    private static Integer currentItineraryId;

    private Session() {}

    public static void login(UserDAO.User user) {
        currentUser = user;
        currentItineraryId = null;
    }

    public static void logout() {
        currentUser = null;
        currentItineraryId = null;
    }

    public static UserDAO.User getUser() { return currentUser; }

    public static boolean isLoggedIn() { return currentUser != null; }

    public static Integer getCurrentItineraryId() { return currentItineraryId; }

    public static void setCurrentItineraryId(Integer itineraryId) { currentItineraryId = itineraryId; }
}