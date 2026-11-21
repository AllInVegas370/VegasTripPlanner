package finalproject.vegastripplanner;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javafx.scene.control.Button;
import java.io.IOException;

public class ItineraryController {
    @FXML
    private Button back;


    @FXML
    protected void onReturnButtonClicked(ActionEvent event) {
        showPage(event, "browse-places.fxml");
    }

    private void showPage(ActionEvent event, String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlName));
            Parent root = loader.load();

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();

            window.setScene(new Scene(root, screenBounds.getWidth(), screenBounds.getHeight()));
            window.setFullScreenExitHint("");
            window.setFullScreen(true);
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
