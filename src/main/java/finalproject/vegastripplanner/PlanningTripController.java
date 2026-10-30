package finalproject.vegastripplanner;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
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
        FXMLLoader loader = new FXMLLoader();
        Parent mainPage = loader.load(MainPageApplication.class.getResource("main-page.fxml"));
        Scene mainScene = new Scene(mainPage);

        Stage window = (Stage) ((Node)event.getSource()).getScene().getWindow();

        window.setScene(mainScene);
        window.setMaximized(true);

        window.show();
    }

    @FXML
    public void initialize(){
        budget.getItems().addAll("0","$10000 - $2000", "$2000 - $3000", "$3000 - $4000", "$4000 - $5000", "$5000 - $6000");
        children.getItems().addAll("1", "2", "3", "4", "5", "6+");
        adults.getItems().addAll("1", "2", "3", "4", "5", "6+");
        interests.getItems().addAll("Hotels", "Restuarants", "Bars", "Casinos", "Shows");
    }
}
