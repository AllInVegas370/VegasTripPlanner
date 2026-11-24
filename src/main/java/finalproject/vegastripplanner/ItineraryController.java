package finalproject.vegastripplanner;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// The itinerary page: the user's trips on the left, the selected trip day by day
// in the middle, and their favorites on the right. Everything comes from AllInVegas.db.
public class ItineraryController {

    @FXML private Button back;
    @FXML private ListView<UserDAO.Itinerary> tripsList;
    @FXML private Label tripTitle;
    @FXML private Label tripDates;
    @FXML private VBox daysBox;
    @FXML private ListView<UserDAO.FavoritePlace> favoritesList;
    @FXML private Button newTripButton, deleteTripButton, addFavoriteButton, removeFavoriteButton;

    // Trips without dates can still spread over this many days
    private static final int MAX_DAYS_WITHOUT_DATES = 14;

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d");
    private static final DateTimeFormatter RANGE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final String SMALL_BUTTON = "-fx-pref-height: 32px; -fx-font-size: 11px; -fx-padding: 0 10 0 10;";


    // ---------------------------------------------------------------
    //  Start-up
    // ---------------------------------------------------------------

    @FXML
    public void initialize() {
        tripsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UserDAO.Itinerary trip, boolean empty) {
                super.updateItem(trip, empty);
                setText(empty || trip == null ? null : trip.name() + "\n" + dateRange(trip));
            }
        });
        favoritesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UserDAO.FavoritePlace fav, boolean empty) {
                super.updateItem(fav, empty);
                setText(empty || fav == null ? null : fav.placeName() + "\n" + capitalize(fav.category()));
            }
        });
        tripsList.setPlaceholder(new Label("No trips yet.\nClick \"New trip\" to start one."));
        favoritesList.setPlaceholder(new Label("No favorites yet.\nUse ♡ Favorite on the browse page."));

        if (!Session.isLoggedIn()) {
            tripTitle.setText("Sign in to see your trips");
            tripDates.setText("Your trips and favorites are saved to your account.");
            for (Button b : List.of(newTripButton, deleteTripButton, addFavoriteButton, removeFavoriteButton)) {
                b.setDisable(true);
            }
            return;
        }

        // Picking a trip shows it and makes it the one "Save to my trip" adds to
        tripsList.getSelectionModel().selectedItemProperty().addListener((obs, oldTrip, trip) -> {
            if (trip != null) Session.setCurrentItineraryId(trip.id());
            showTrip(trip);
        });

        loadTrips();
        loadFavorites();
    }

    private int userId() {
        return Session.getUser().id();
    }


    // ---------------------------------------------------------------
    //  Trips list
    // ---------------------------------------------------------------

    // Reloads the trips and re-selects the current one (or the newest)
    private void loadTrips() {
        List<UserDAO.Itinerary> trips;
        try {
            trips = UserDAO.getItineraries(userId());
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Couldn't load your trips.");
            return;
        }
        tripsList.getItems().setAll(trips);

        if (trips.isEmpty()) {
            showTrip(null);
            return;
        }
        Integer currentId = Session.getCurrentItineraryId();
        UserDAO.Itinerary toSelect = trips.get(0);
        for (UserDAO.Itinerary t : trips) {
            if (currentId != null && t.id() == currentId) toSelect = t;
        }
        tripsList.getSelectionModel().select(toSelect);
        showTrip(toSelect);   // also refreshes if the same trip was already selected
    }

    @FXML
    protected void onNewTripClicked() {
        TextInputDialog dialog = new TextInputDialog("My Vegas Trip");
        dialog.setTitle("New trip");
        dialog.setHeaderText(null);
        dialog.setContentText("Trip name:");
        Optional<String> name = dialog.showAndWait();
        if (name.isEmpty()) return;   // cancelled

        try {
            int id = UserDAO.createItinerary(userId(), name.get(), null, null);
            Session.setCurrentItineraryId(id);
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Couldn't create the trip.");
        }
        loadTrips();
    }

    @FXML
    protected void onDeleteTripClicked() {
        UserDAO.Itinerary trip = tripsList.getSelectionModel().getSelectedItem();
        if (trip == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + trip.name() + "\" and everything planned in it?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            UserDAO.deleteItinerary(trip.id());   // its stops are deleted automatically
            Session.setCurrentItineraryId(null);
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Couldn't delete the trip.");
        }
        loadTrips();
    }


    // ---------------------------------------------------------------
    //  Selected trip, day by day
    // ---------------------------------------------------------------

    private void showTrip(UserDAO.Itinerary trip) {
        daysBox.getChildren().clear();
        deleteTripButton.setDisable(trip == null);
        addFavoriteButton.setDisable(trip == null);

        if (trip == null) {
            tripTitle.setText("No trip selected");
            tripDates.setText("Start one with \"New trip\", or use Plan a Trip → Find my Vegas.");
            return;
        }
        tripTitle.setText(trip.name());
        tripDates.setText(dateRange(trip));

        List<UserDAO.ItineraryItem> items;
        try {
            items = UserDAO.getItineraryItems(trip.id());   // already sorted by day, then position
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Couldn't load this trip.");
            return;
        }

        int dayCount = dayCount(trip, items);
        for (int day = 1; day <= dayCount; day++) {
            Label header = new Label(dayHeader(trip, day));
            header.getStyleClass().add("section-title");
            daysBox.getChildren().add(header);

            List<UserDAO.ItineraryItem> stops = new ArrayList<>();
            for (UserDAO.ItineraryItem item : items) {
                if (item.dayNumber() == day) stops.add(item);
            }

            if (stops.isEmpty()) {
                Label empty = new Label("Nothing planned yet. Add places from Browse places or your favorites.");
                empty.getStyleClass().add("section-hint");
                daysBox.getChildren().add(empty);
            }
            for (UserDAO.ItineraryItem stop : stops) {
                daysBox.getChildren().add(stopRow(trip, stop, dayCount));
            }
        }
    }

    // One stop: name, buttons to move it a day earlier/later, and Remove
    private HBox stopRow(UserDAO.Itinerary trip, UserDAO.ItineraryItem stop, int dayCount) {
        Label name = new Label(stop.placeName());
        name.getStyleClass().add("place-name");
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);

        Button earlier = smallButton("◀ Day " + (stop.dayNumber() - 1));
        earlier.setDisable(stop.dayNumber() <= 1);
        earlier.setOnAction(e -> moveStop(trip, stop, stop.dayNumber() - 1));

        Button later = smallButton("Day " + (stop.dayNumber() + 1) + " ▶");
        later.setDisable(stop.dayNumber() >= maxDays(trip));
        later.setOnAction(e -> moveStop(trip, stop, stop.dayNumber() + 1));

        Button remove = smallButton("Remove");
        remove.setOnAction(e -> {
            try {
                UserDAO.removeItineraryItem(stop.itemId());
            } catch (SQLException ex) {
                ex.printStackTrace();
                showMessage("Couldn't remove that stop.");
            }
            showTrip(trip);
        });

        if (stop.dayNumber() <= 1) earlier.setText("◀");

        HBox row = new HBox(10, name, earlier, later, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("place-card");
        return row;
    }

    private void moveStop(UserDAO.Itinerary trip, UserDAO.ItineraryItem stop, int newDay) {
        try {
            UserDAO.moveItemToDay(stop.itemId(), newDay);
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Couldn't move that stop.");
        }
        showTrip(trip);
    }

    private static Button smallButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("secondary-button");
        b.setStyle(SMALL_BUTTON);
        return b;
    }

    // Trips with dates show every day of the trip; trips without dates show up to the last used day
    private static int dayCount(UserDAO.Itinerary trip, List<UserDAO.ItineraryItem> items) {
        if (trip.startDate() != null && trip.endDate() != null) {
            return maxDays(trip);
        }
        int lastUsed = 1;
        for (UserDAO.ItineraryItem item : items) lastUsed = Math.max(lastUsed, item.dayNumber());
        return lastUsed;
    }

    private static int maxDays(UserDAO.Itinerary trip) {
        if (trip.startDate() != null && trip.endDate() != null) {
            return Math.max(1, (int) ChronoUnit.DAYS.between(trip.startDate(), trip.endDate()) + 1);
        }
        return MAX_DAYS_WITHOUT_DATES;
    }

    // "Day 2 · Sat, Dec 5" when the trip has a start date, otherwise just "Day 2"
    private static String dayHeader(UserDAO.Itinerary trip, int day) {
        if (trip.startDate() == null) return "Day " + day;
        LocalDate date = trip.startDate().plusDays(day - 1);
        return "Day " + day + " · " + date.format(DAY_FORMAT);
    }

    private static String dateRange(UserDAO.Itinerary trip) {
        if (trip.startDate() == null) return "No dates yet";
        if (trip.endDate() == null) return "From " + trip.startDate().format(RANGE_FORMAT);
        return trip.startDate().format(RANGE_FORMAT) + " – " + trip.endDate().format(RANGE_FORMAT);
    }


    // ---------------------------------------------------------------
    //  Favorites
    // ---------------------------------------------------------------

    private void loadFavorites() {
        try {
            favoritesList.getItems().setAll(UserDAO.getFavorites(userId()));
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Couldn't load your favorites.");
        }
    }

    @FXML
    protected void onAddFavoriteToTripClicked() {
        UserDAO.FavoritePlace fav = favoritesList.getSelectionModel().getSelectedItem();
        UserDAO.Itinerary trip = tripsList.getSelectionModel().getSelectedItem();
        if (fav == null) {
            showMessage("Pick a favorite first.");
            return;
        }
        if (trip == null) {
            showMessage("Pick or create a trip first.");
            return;
        }

        try {
            if (UserDAO.getPlaceIdsInItinerary(trip.id()).contains(fav.placeId())) {
                showMessage(fav.placeName() + " is already in this trip.");
                return;
            }
            UserDAO.addPlaceToItinerary(trip.id(), fav.placeId(), 1);
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Couldn't add that place.");
        }
        showTrip(trip);
    }

    @FXML
    protected void onRemoveFavoriteClicked() {
        UserDAO.FavoritePlace fav = favoritesList.getSelectionModel().getSelectedItem();
        if (fav == null) return;
        try {
            UserDAO.removeFavorite(userId(), fav.placeId());
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Couldn't remove that favorite.");
        }
        loadFavorites();
    }


    // ---------------------------------------------------------------
    //  Navigation and helpers
    // ---------------------------------------------------------------

    @FXML
    protected void onReturnButtonClicked(ActionEvent event) {
        showPage(event, "browse-places.fxml");
    }

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

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}