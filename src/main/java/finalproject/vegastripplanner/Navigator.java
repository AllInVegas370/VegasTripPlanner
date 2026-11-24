package finalproject.vegastripplanner;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.io.IOException;

/**
 * Switches the window to another page.
 * It swaps the page inside the window that is already open (scene.setRoot),
 * so the window stays full screen and nothing flickers.
 */
public final class Navigator {

    public static final String WELCOME = "main-page.fxml";
    public static final String CREATE_ACCOUNT = "account.fxml";
    public static final String PREFERENCES = "planning-trip.fxml";
    public static final String BROWSE = "browse-places.fxml";
    public static final String DASHBOARD = "dashboard.fxml";
    public static final String ACCOUNT_SETTINGS = "account-settings.fxml";
    public static final String ITINERARY = "itinerary.fxml";

    private Navigator() {}

    /** Shows the page in the same window as {@code anyNodeOnCurrentPage}. Returns its controller (or null if it failed). */
    public static <T> T go(Node anyNodeOnCurrentPage, String fxmlName) {
        if (anyNodeOnCurrentPage == null || anyNodeOnCurrentPage.getScene() == null) return null;
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlName));
            Parent root = loader.load();
            anyNodeOnCurrentPage.getScene().setRoot(root);
            return loader.getController();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            Ui.info(anyNodeOnCurrentPage, "Couldn't open that page (" + fxmlName + "). See the console for details.");
            return null;
        }
    }
}