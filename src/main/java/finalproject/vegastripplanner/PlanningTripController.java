package finalproject.vegastripplanner;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;

public class PlanningTripController {

    @FXML
    private ChoiceBox<String> partyNumber, budgetAmount, averageAge, interests;

    @FXML
    public void initialize(){
        partyNumber.getItems().addAll("1", "2", "3", "4", "5 or more");
        budgetAmount.getItems().addAll("$100-$200", "$200-$300");
        averageAge.getItems().addAll("1-5 years", "5-10 years", "10-18 years", "18-20 years", "21+ years");
        interests.getItems().addAll("Bars", "Casinos");
    }
}
