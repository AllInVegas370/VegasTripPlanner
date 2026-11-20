package finalproject.vegastripplanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class PlanningTripController {

    // ----- header buttons -----
    @FXML
    private Button quitButton, returnButton;

    // ----- interests: each one can be on or off, so several can be picked -----
    @FXML
    private ToggleButton diningToggle, entertainmentToggle, attractionsToggle,
            casinosToggle, hotelsToggle, shoppingToggle;

    // ----- budget: three cards, only one can be selected at a time -----
    @FXML
    private ToggleGroup budgetGroup;

    // ----- party size: number labels with - and + buttons -----
    @FXML
    private Label adultsValue, childrenValue;

    @FXML
    private Button adultsMinusButton, adultsPlusButton,
            childrenMinusButton, childrenPlusButton;

    // ----- dates -----
    @FXML
    private DatePicker startDate, endDate;

    private Trip userTrip;

    private int adultCount = 2;
    private int childCount = 0;

    private static final int MAX_GUESTS = 10;


    @FXML
    public void initialize() {
        // Start with the middle budget card selected, like the design.
        budgetGroup.selectToggle(budgetGroup.getToggles().get(1));

        // Clicking the selected card again would normally turn it off.
        // This puts it back on, so a budget is always chosen.
        budgetGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                budgetGroup.selectToggle(oldToggle);
            }
        });

        updateCounts();
    }


    // ---------------------------------------------------------------
    //  Party size buttons
    // ---------------------------------------------------------------

    @FXML
    protected void onAdultsMinusClicked() {
        adultCount--;
        updateCounts();
    }

    @FXML
    protected void onAdultsPlusClicked() {
        adultCount++;
        updateCounts();
    }

    @FXML
    protected void onChildrenMinusClicked() {
        childCount--;
        updateCounts();
    }

    @FXML
    protected void onChildrenPlusClicked() {
        childCount++;
        updateCounts();
    }

    // Shows the current numbers and greys out a button when it hits its limit.
    private void updateCounts() {
        adultsValue.setText(String.valueOf(adultCount));
        childrenValue.setText(String.valueOf(childCount));

        adultsMinusButton.setDisable(adultCount <= 1);          // at least 1 adult
        adultsPlusButton.setDisable(adultCount >= MAX_GUESTS);
        childrenMinusButton.setDisable(childCount <= 0);
        childrenPlusButton.setDisable(childCount >= MAX_GUESTS);
    }


    // ---------------------------------------------------------------
    //  Navigation
    // ---------------------------------------------------------------

    @FXML
    protected void onQuitButtonClicked(){
        Platform.exit();
    }

    @FXML
    protected void onReturnButtonClicked(ActionEvent event) {
        showPage(event, "main-page.fxml");
    }

    // "Skip for now - browse places": go to browsing without saving a trip.
    @FXML
    protected void onSkipButtonClicked(ActionEvent event) {
        showBrowsingPage(event);
    }

    // "Find my Vegas": save the choices, then go to browsing.
    @FXML
    protected void onSubmitButtonClicked(ActionEvent event) {
        LocalDate userStartDate = startDate.getValue();   // null if left empty
        LocalDate userEndDate = endDate.getValue();

        // Dates are optional ("It's okay if you're not sure yet"),
        // but if both are filled in they have to make sense.
        if (userStartDate != null && userEndDate != null && userEndDate.isBefore(userStartDate)) {
            showMessage("Your departure date can't be before your arrival date.");
            return;
        }

        userTrip = new Trip(
                getBudgetText(),
                String.valueOf(childCount),
                String.valueOf(adultCount),
                getSelectedInterests(),
                userStartDate,
                userEndDate);
        objsToFile(userTrip);

        System.out.println("Saved trip, switching screens");

        showBrowsingPage(event);
    }

    // Loads an FXML page into the current window, full screen.
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

    // Opens the browse-places page in the current window, full screen.
    private void showBrowsingPage(ActionEvent event) {
        showPage(event, "browse-places.fxml");
    }

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }


    // ---------------------------------------------------------------
    //  Reading the choices
    // ---------------------------------------------------------------

    // Turns the selected budget card into readable text for the saved file.
    private String getBudgetText() {
        Toggle chosen = budgetGroup.getSelectedToggle();
        if (chosen == null) {
            return "";
        }
        String code = (String) chosen.getUserData();
        return switch (code) {
            case "UNDER_150" -> "Under $150";
            case "150_300" -> "$150-$300";
            default -> "$300+";
        };
    }

    // Collects every interest that is switched on, e.g. "Dining, Shows".
    // An empty string means no interests were picked.
    private String getSelectedInterests() {
        List<String> picked = new ArrayList<>();
        List<ToggleButton> all = List.of(diningToggle, entertainmentToggle, attractionsToggle,
                casinosToggle, hotelsToggle, shoppingToggle);
        for (ToggleButton interest : all) {
            if (interest.isSelected()) {
                picked.add(interest.getText());
            }
        }
        return String.join(", ", picked);
    }

    public static class Trip
    {
        private String budget;
        private String children;
        private String adults;
        private String interests;
        private LocalDate startDate;
        private LocalDate endDate;

        public Trip(){}

        public Trip(String budget, String children,
                    String adults, String interests, LocalDate startDate, LocalDate endDate)
        {
            this.budget = budget;//Show only ranges of prices to user
            this.children = children;
            this.adults = adults; //Same as children
            this.interests = interests;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public String getBudget(){return budget;}
        public String getChildren(){return children;}
        public String getAdults(){return adults;}
        public String getInterests(){return interests;}
        public LocalDate getStartDate(){return startDate;}
        public LocalDate getEndDate(){return endDate;}

    }

    public void objsToFile( Trip trip)
    {

        String filePath = "userData/trip.json";
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(filePath);
        mapper.registerModule(new JavaTimeModule());//Handles LocalDate[Start and End dates]
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {

            file.getParentFile().mkdirs();
            /*
            Save for later to link usernames to objects
            Map<String, Trip> trips = new HashMap<>();
            if(file.exists() && file.length() > 0) {
                trips = mapper.readValue(file, new TypeReference<HashMap<String, Trip>>(){});
            }

            if(trips.containsKey(username)){
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());//Handles LocalDate[Start and End dates]
            */
            List<Trip> trips = new ArrayList<>();
            if(file.exists() && file.length() > 0){
                trips = mapper.readValue(file, new TypeReference<ArrayList<Trip>>(){});
            }
            if(trips.contains(trip)){
                return;
            }
            trips.add(trip);
            mapper.writeValue(file, trips);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}