package finalproject.vegastripplanner;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class PlanningTripController {

    @FXML
    private Button quitButton, returnButton;

    @FXML
    protected void onQuitButtonClicked(){
        Platform.exit();
    }

    @FXML
    protected void onReturnButtonClicked(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        Parent mainPage = loader.load(MainPageApplication.class.getResource("main-page.fxml"));
        Scene mainScene = new Scene(mainPage);

        Stage window = (Stage) ((Node)event.getSource()).getScene().getWindow();

        window.setScene(mainScene);

        window.show();
    }
}
