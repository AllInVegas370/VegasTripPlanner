package finalproject.vegastripplanner;

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

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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

    private int adultCount = 2;
    private int childCount = 0;

    private static final int MAX_GUESTS = 10;

    // Budget card (its userData in planning-trip.fxml) -> price_ID in priceTable
    //   priceTable: 1 = Free, 2 = $, 3 = $$, 4 = $$$
    private static final Map<String, Integer> BUDGET_TO_PRICE_ID = Map.of(
            "UNDER_150", 2,
            "150_300", 3,
            "OVER_300", 4);

    // Interest card -> category_ID in categoryTable
    //   1 restaurant, 2 bar, 3 hotel, 4 entertainment, 5 shopping, 6 casino, 7 attraction
    private Map<ToggleButton, Integer> interestCategories;


    @FXML
    public void initialize() {
        interestCategories = Map.of(
                diningToggle, 1,
                entertainmentToggle, 4,
                attractionsToggle, 7,
                casinosToggle, 6,
                hotelsToggle, 3,
                shoppingToggle, 5);

        // Start with the middle budget card selected, like the design.
        budgetGroup.selectToggle(budgetGroup.getToggles().get(1));

        // Clicking the selected card again would normally turn it off.
        // This puts it back on, so a budget is always chosen.
        budgetGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                budgetGroup.selectToggle(oldToggle);
            }
        });

        loadSavedPreferences();
        updateCounts();
    }

    // If the signed-in user saved preferences before, fill the page in with them.
    private void loadSavedPreferences() {
        if (!Session.isLoggedIn()) return;

        UserDAO.Preferences saved;
        try {
            saved = UserDAO.getPreferences(Session.getUser().id());
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        if (saved == null) return;   // nothing saved yet: keep the defaults

        adultCount = saved.adults();
        childCount = saved.children();
        startDate.setValue(saved.arrival());
        endDate.setValue(saved.departure());

        for (Map.Entry<ToggleButton, Integer> entry : interestCategories.entrySet()) {
            entry.getKey().setSelected(saved.interestCategoryIds().contains(entry.getValue()));
        }

        if (saved.budgetPriceId() != null) {
            for (Toggle card : budgetGroup.getToggles()) {
                if (saved.budgetPriceId().equals(BUDGET_TO_PRICE_ID.get((String) card.getUserData()))) {
                    budgetGroup.selectToggle(card);
                }
            }
        }
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

    // "Skip for now - browse places": go to browsing without saving anything.
    @FXML
    protected void onSkipButtonClicked(ActionEvent event) {
        showBrowsingPage(event);
    }

    // "Find my Vegas": save the preferences, start a new trip with these dates, then go to browsing.
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

        if (!Session.isLoggedIn()) {
            showMessage("Sign in to save your preferences and trips. For now, here are places to browse.");
            showBrowsingPage(event);
            return;
        }

        int userId = Session.getUser().id();
        try {
            UserDAO.savePreferences(userId, getBudgetPriceId(), adultCount, childCount,
                    userStartDate, userEndDate, getSelectedCategoryIds());

            // Each "Find my Vegas" starts a new trip; "Save to my trip" on the browse page adds to it.
            int tripId = UserDAO.createItinerary(userId, tripName(userStartDate, userEndDate),
                    userStartDate, userEndDate);
            Session.setCurrentItineraryId(tripId);
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Couldn't save your trip. Please try again.");
            return;
        }

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
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }


    // ---------------------------------------------------------------
    //  Reading the choices
    // ---------------------------------------------------------------

    // The selected budget card as a price_ID (null if somehow none is selected)
    private Integer getBudgetPriceId() {
        Toggle chosen = budgetGroup.getSelectedToggle();
        return chosen == null ? null : BUDGET_TO_PRICE_ID.get((String) chosen.getUserData());
    }

    // The category_IDs of every interest that is switched on (empty = none picked)
    private List<Integer> getSelectedCategoryIds() {
        List<Integer> picked = new ArrayList<>();
        for (Map.Entry<ToggleButton, Integer> entry : interestCategories.entrySet()) {
            if (entry.getKey().isSelected()) {
                picked.add(entry.getValue());
            }
        }
        // Dining covers bars too
        if (diningToggle.isSelected()) {
            picked.add(2);
        }
        return picked;
    }

    // e.g. "Vegas Trip · Dec 4 – Dec 7", or "My Vegas Trip" when there are no dates yet
    private static String tripName(LocalDate start, LocalDate end) {
        if (start == null) return "My Vegas Trip";
        DateTimeFormatter f = DateTimeFormatter.ofPattern("MMM d");
        return end == null
                ? "Vegas Trip · " + start.format(f)
                : "Vegas Trip · " + start.format(f) + " – " + end.format(f);
    }
}