package finalproject.vegastripplanner;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;

public class PlanningTripController {

    @FXML
    private ChoiceBox<String> partyNumber;

    @FXML
    public void initialize(){
        partyNumber.getItems().addAll("1", "2", "3", "4", "5 or more");
    }
}
