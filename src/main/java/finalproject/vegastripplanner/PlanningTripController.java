package finalproject.vegastripplanner;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;

public class PlanningTripController {

    @FXML
    private ChoiceBox<String> partyNumber, budgetAmount, adultAmount, interests, kidAmount;

    @FXML
    public void initialize(){
        partyNumber.getItems().addAll("1", "2", "3", "4", "5 or more");
        budgetAmount.getItems().addAll("$1000-$2000", "$2000-$3000", "$3000-$4000", "$4000-$5000");
        adultAmount.getItems().addAll("2", "4", "5", "6+", "10+");
        kidAmount.getItems().addAll("2", "4", "6+");
        interests.getItems().addAll("Bars", "Casinos", "Restaurants", "Attractions");
    }
}
