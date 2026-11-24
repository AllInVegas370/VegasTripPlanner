package finalproject.vegastripplanner;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.util.List;

/**
 * The top bar shared by the signed-in pages (nav-bar.fxml).
 * A page that includes it gets this controller in a field named navBarController
 * and calls setActive(...) so the right button is highlighted.
 */
public class NavBarController {

    public static final String BROWSE = "browse";
    public static final String DASHBOARD = "dashboard";
    public static final String ACCOUNT = "account";

    private static final String NORMAL = "aiv-nav-link";
    private static final String ACTIVE = "aiv-nav-link-active";

    @FXML private Button browseNavButton, dashboardNavButton, accountNavButton, logOutButton;

    @FXML
    public void initialize() {
        // Guests (who came in with "Browse as a guest") see "Sign in" instead of "Log out"
        logOutButton.setText(Session.isLoggedIn() ? "Log out" : "Sign in");
    }

    /** Highlights one of BROWSE, DASHBOARD or ACCOUNT. */
    public void setActive(String page) {
        setStyle(browseNavButton, BROWSE.equals(page));
        setStyle(dashboardNavButton, DASHBOARD.equals(page));
        setStyle(accountNavButton, ACCOUNT.equals(page));
    }

    private static void setStyle(Button button, boolean active) {
        button.getStyleClass().removeAll(List.of(NORMAL, ACTIVE));
        button.getStyleClass().add(active ? ACTIVE : NORMAL);
    }

    @FXML
    protected void onBrowseClicked() {
        Navigator.go(browseNavButton, Navigator.BROWSE);
    }

    @FXML
    protected void onDashboardClicked() {
        if (signedInOrAsk("Sign in to see your dashboard, trips and favorites.")) {
            Navigator.go(dashboardNavButton, Navigator.DASHBOARD);
        }
    }

    @FXML
    protected void onAccountClicked() {
        if (signedInOrAsk("Sign in to change your account settings.")) {
            Navigator.go(accountNavButton, Navigator.ACCOUNT_SETTINGS);
        }
    }

    @FXML
    protected void onLogOutClicked() {
        Session.logout();
        Navigator.go(logOutButton, Navigator.WELCOME);
    }

    // Guests get asked whether they want to go sign in
    private boolean signedInOrAsk(String message) {
        if (Session.isLoggedIn()) return true;
        if (Ui.confirm(logOutButton, message, "Sign in")) {
            Navigator.go(logOutButton, Navigator.WELCOME);
        }
        return false;
    }
}