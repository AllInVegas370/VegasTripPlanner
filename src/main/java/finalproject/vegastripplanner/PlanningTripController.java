package finalproject.vegastripplanner;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;

public class PlanningTripController {

    @FXML
    private Button quitButton, returnButton;

    @FXML
    private ChoiceBox<String> budget, children, adults, interests;

    @FXML
    protected void onQuitButtonClicked(){
        Platform.exit();
    }

    @FXML
    protected void onReturnButtonClicked(ActionEvent event) throws IOException {
        try {
            // 1. Load your Scene Builder FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-page.fxml"));
            Parent root = loader.load();

            // 2. Fetch the team member's monitor screen bounds
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 3. Instantiate the scene using their exact screen dimensions right away
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            window.setScene(scene);

            // 4. PRE-CONFIGURE full-screen states BEFORE showing the window
            window.setFullScreenExitHint(""); // Removes the annoying popup text
            window.setFullScreen(true);       // Arms the full-screen flag early

            // 5. Finally, reveal the window
            // It will now render instantly in full-screen without the "shrink and snap"
            window.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize(){
        budget.getItems().addAll("0","$10000 - $2000", "$2000 - $3000", "$3000 - $4000", "$4000 - $5000", "$5000 - $6000");
        children.getItems().addAll("1", "2", "3", "4", "5", "6+");
        adults.getItems().addAll("1", "2", "3", "4", "5", "6+");
        interests.getItems().addAll("Hotels", "Restuarants", "Bars", "Casinos", "Shows");
    }
}
