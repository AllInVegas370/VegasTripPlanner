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

public class MainController {
    @FXML
    private Button quitButton, planTrpButton, exploreButton, createAccountButton;

    // This buttons quits the entire program
    @FXML
    protected void onQuitButtonClicked(){
        Platform.exit();
    }

    // This buttons connects to PlanningTripApplication
    @FXML
    protected void onPlanTripButtonClicked(ActionEvent event) throws IOException {
        // Loading FXML and setting the loader to the application
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(PlanningTripApplication.class.getResource("planning-trip.fxml"));
        Parent planningViewer = loader.load();

        // Creating the new scene so that the scene shows up
        Scene planScene = new Scene(planningViewer);

        Stage window = (Stage) ((Node)event.getSource()).getScene().getWindow();

        window.setScene(planScene);

        // setMaximized sets the window to appear in full screen mode and adjusts based on users screen size
        window.setMaximized(true);

        window.show();
    }

    // This buttons connects to ExploringApplication
    @FXML
    protected void onExploreButtonClicked(ActionEvent event) throws IOException {
        // Loading FXML and setting the loader to the application
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ExploringApplication.class.getResource("exploring.fxml"));
        Parent exploreViewer = loader.load();

        // Creating the new scene so that the scene shows up
        Scene exploreScene = new Scene(exploreViewer);

        Stage window = (Stage) ((Node)event.getSource()).getScene().getWindow();

        window.setScene(exploreScene);

        // setMaximized sets the window to appear in full screen mode and adjusts based on users screen size
        window.setMaximized(true);

        window.show();
    }

    // This buttons connects to AccountApplication so that a user can make a new account
    @FXML
    protected void onCreateAccountButtonClicked(ActionEvent event) throws IOException{
        // Loading FXML and setting the loader to the application
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ExploringApplication.class.getResource("account.fxml"));
        Parent accountViewer = loader.load();

        // Creating the new scene so that the scene shows up
        Scene accountScene = new Scene(accountViewer);

        Stage window = (Stage) ((Node)event.getSource()).getScene().getWindow();

        window.setScene(accountScene);

        // setMaximized sets the window to appear in full screen mode and adjusts based on users screen size
        window.setMaximized(true);

        window.show();
    }
}
