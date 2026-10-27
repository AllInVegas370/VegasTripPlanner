package finalproject.vegastripplanner;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class MainController {
    @FXML
    private Button quitButton, planTrpButton, exploreButton;

    @FXML
    protected void onQuitButtonClicked(){
        Platform.exit();
    }

    @FXML
    protected void onPlanTripButtonClicked(ActionEvent event){
        System.out.println("Planned Trip");
    }

    @FXML
    protected void onExploreButtonClicked(ActionEvent event){
        System.out.println("Exploring");
    }
}
