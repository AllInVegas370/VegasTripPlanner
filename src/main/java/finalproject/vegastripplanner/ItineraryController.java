package finalproject.vegastripplanner;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Itinerary page (itinerary.fxml): one trip, day by day, with a map of the selected day.
// Which trip: Session.getCurrentItineraryId() (set by the dashboard's trip cards),
// otherwise the user's newest trip.
public class ItineraryController {

    private static final double PHOTO_WIDTH = 120;
    private static final double STOP_HEIGHT = 148;
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("EEEE, MMM d");

    @FXML private NavBarController navBarController;   // the included nav-bar.fxml
    @FXML private Label breadcrumbLabel, tripTitle, tripSubtitle;
    @FXML private Button addPlaceButton, renameButton, deleteTripButton, dashboardCrumb;
    @FXML private FlowPane dayTabs;
    @FXML private Label dayTitle, daySubtitle, dayCountLabel;
    @FXML private VBox stopsBox;
    @FXML private StackPane mapHolder;
    @FXML private Label mapSubtitle, mapSummaryTitle, firstStopLabel, lastStopLabel, travelersLabel;

    private final Map<Integer, Place> placesById = new PlaceDao().getPlacesById();
    private final ItineraryMap map = new ItineraryMap();

    private UserDAO.Itinerary trip;
    private List<UserDAO.ItineraryItem> items = List.of();
    private int selectedDay = 1;
    private int travelers;


    // ---------------------------------------------------------------
    //  Start-up
    // ---------------------------------------------------------------

    @FXML
    public void initialize() {
        navBarController.setActive(NavBarController.DASHBOARD);

        // The map keeps the design's 660 x 540 shape at any width
        mapHolder.getChildren().add(map);
        mapHolder.prefHeightProperty().bind(mapHolder.widthProperty().multiply(ItineraryMap.ASPECT));

        if (!Session.isLoggedIn()) {
            showNoTrip("Sign in to see your trips", "Your trips and favorites are saved to your account.");
            return;
        }
        travelers = Trips.travelers(Session.getUser().id());
        travelersLabel.setText(travelers > 0 ? Trips.plural(travelers, "traveler") : "—");
        loadTrip();
    }

    // Finds the trip to show: the one picked on the dashboard, or the newest
    private void loadTrip() {
        List<UserDAO.Itinerary> trips;
        try {
            trips = UserDAO.getItineraries(Session.getUser().id());   // newest first
        } catch (SQLException e) {
            e.printStackTrace();
            showNoTrip("Couldn't load your trip", "Please try again.");
            return;
        }
        if (trips.isEmpty()) {
            showNoTrip("No trips yet", "Create one from My dashboard with \"+ Create itinerary\".");
            return;
        }

        Integer wanted = Session.getCurrentItineraryId();
        trip = trips.get(0);
        for (UserDAO.Itinerary t : trips) {
            if (wanted != null && t.id() == wanted) trip = t;
        }
        Session.setCurrentItineraryId(trip.id());   // "Save to my trip" now adds to this one
        refresh();
    }

    private void showNoTrip(String title, String text) {
        trip = null;
        tripTitle.setText(title);
        tripSubtitle.setText(text);
        breadcrumbLabel.setText("");
        for (Button b : List.of(addPlaceButton, renameButton, deleteTripButton)) b.setDisable(true);
        dayTabs.getChildren().clear();
        stopsBox.getChildren().clear();
        dayTitle.setText("");
        daySubtitle.setText("");
        dayCountLabel.setText("");
        mapSummaryTitle.setText("Nothing to map yet.");
        map.show(List.of(), 0);
    }


    // ---------------------------------------------------------------
    //  Whole trip
    // ---------------------------------------------------------------

    // Reloads the stops and redraws everything (keeps the selected day if it still exists)
    private void refresh() {
        try {
            items = UserDAO.getItineraryItems(trip.id());   // sorted by day, then position
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(stopsBox, "Couldn't load this trip.", true);
            items = List.of();
        }

        breadcrumbLabel.setText("/  " + trip.name().toUpperCase());
        tripTitle.setText(trip.name());
        List<String> subtitle = new ArrayList<>(List.of("Las Vegas, Nevada", Trips.dateRange(trip)));
        if (travelers > 0) subtitle.add(Trips.plural(travelers, "traveler"));
        tripSubtitle.setText(String.join("  ·  ", subtitle));

        int dayCount = Trips.dayCount(trip, items);
        selectedDay = Math.max(1, Math.min(selectedDay, dayCount));
        buildDayTabs(dayCount);
        showDay(selectedDay);
    }

    private void buildDayTabs(int dayCount) {
        dayTabs.getChildren().clear();
        ToggleGroup group = new ToggleGroup();
        for (int day = 1; day <= dayCount; day++) {
            Label name = new Label(Trips.dayTab(trip, day));
            name.getStyleClass().add("itin-day-name");
            Label count = new Label(Trips.plural(stopsOn(day).size(), "place"));
            count.getStyleClass().add("itin-day-places");

            ToggleButton tab = new ToggleButton();
            tab.setGraphic(new VBox(2, name, count));
            tab.getStyleClass().add("itin-day");
            tab.setToggleGroup(group);
            tab.setSelected(day == selectedDay);
            int thisDay = day;
            tab.setOnAction(e -> {
                tab.setSelected(true);   // clicking the open day again keeps it open
                showDay(thisDay);
            });
            dayTabs.getChildren().add(tab);
        }
    }

    private List<UserDAO.ItineraryItem> stopsOn(int day) {
        List<UserDAO.ItineraryItem> stops = new ArrayList<>();
        for (UserDAO.ItineraryItem item : items) {
            if (item.dayNumber() == day) stops.add(item);
        }
        return stops;
    }


    // ---------------------------------------------------------------
    //  One day: stop cards + map
    // ---------------------------------------------------------------

    private void showDay(int day) {
        selectedDay = day;
        List<UserDAO.ItineraryItem> stops = stopsOn(day);

        dayTitle.setText(Trips.dayTitle(trip, day));
        LocalDate date = Trips.dateOf(trip, day);
        daySubtitle.setText((date != null ? date.format(FULL_DATE) + "  ·  " : "")
                + Trips.plural(stops.size(), "place")
                + (stops.isEmpty() ? "" : "  ·  A little wonder, a lot of Vegas."));
        dayCountLabel.setText("Day " + day + " of " + Trips.dayCount(trip, items));

        stopsBox.getChildren().clear();
        if (stops.isEmpty()) {
            stopsBox.getChildren().add(emptyDayCard());
        }
        for (int i = 0; i < stops.size(); i++) {
            stopsBox.getChildren().add(stopCard(stops.get(i), i + 1));
        }

        updateMap(stops);
    }

    private HBox stopCard(UserDAO.ItineraryItem stop, int number) {
        Place place = placesById.get(stop.placeId());

        ImageView photo = Ui.coverImage(Ui.placeImage(stop.placeId(), 260), PHOTO_WIDTH, STOP_HEIGHT);

        // number · category · price
        Label badge = new Label(String.valueOf(number));
        badge.getStyleClass().add("aiv-number");
        String categoryText = place == null ? "" : Ui.categoryLabel(place.category()).toUpperCase();
        if (place != null && place.priceLevel() != null) categoryText += "  ·  " + place.priceLevel();
        Label category = new Label(categoryText);
        category.getStyleClass().add("itin-category");
        HBox meta = new HBox(10, badge, category);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(stop.placeName());
        name.getStyleClass().add("itin-stop-name");
        name.setWrapText(true);

        String address = place == null ? "" : Ui.shortAddress(place.address());
        Label addressLabel = new Label(address.isEmpty() ? "Address not listed" : address);
        addressLabel.getStyleClass().add("itin-stop-text");

        // move earlier / later, remove, view
        int maxDays = Trips.maxDays(trip);
        Button earlier = smallAction("◀ Day " + (stop.dayNumber() - 1));
        earlier.setVisible(stop.dayNumber() > 1);
        earlier.setManaged(stop.dayNumber() > 1);
        earlier.setOnAction(e -> moveStop(stop, stop.dayNumber() - 1));
        Button later = smallAction("Day " + (stop.dayNumber() + 1) + " ▶");
        later.setVisible(stop.dayNumber() < maxDays);
        later.setManaged(stop.dayNumber() < maxDays);
        later.setOnAction(e -> moveStop(stop, stop.dayNumber() + 1));
        Button remove = smallAction("Remove");
        remove.setOnAction(e -> removeStop(stop));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button view = new Button("View place  →");
        view.getStyleClass().add("itin-stop-link");
        view.setOnAction(e -> {
            BrowsePlacesController browse = Navigator.go(view, Navigator.BROWSE);
            if (browse != null) browse.showPlace(stop.placeId());
        });
        HBox footer = new HBox(16, earlier, later, remove, spacer, view);
        footer.setAlignment(Pos.CENTER_LEFT);

        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);
        VBox body = new VBox(meta, name, addressLabel, push, footer);
        body.getStyleClass().add("itin-stop-body");
        body.setMinWidth(0);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox card = new HBox(photo, body);
        card.getStyleClass().addAll("aiv-card", "itin-stop");
        card.setMinHeight(STOP_HEIGHT);
        Ui.roundCorners(card, 8);
        return card;
    }

    private VBox emptyDayCard() {
        Label title = new Label("Nothing planned for this day yet.");
        title.getStyleClass().add("aiv-card-title");
        Label text = new Label("Add places from Browse places, or move a stop here from another day.");
        text.getStyleClass().add("aiv-muted");
        text.setWrapText(true);
        Button add = new Button("+  Add a place");
        add.getStyleClass().add("aiv-button");
        add.setOnAction(e -> onAddPlaceClicked());
        VBox card = new VBox(title, text, add);
        card.getStyleClass().addAll("aiv-card", "itin-empty");
        return card;
    }

    private static Button smallAction(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("itin-small-action");
        return b;
    }

    private void updateMap(List<UserDAO.ItineraryItem> stops) {
        List<ItineraryMap.Stop> pins = new ArrayList<>();
        int withoutLocation = 0;
        for (int i = 0; i < stops.size(); i++) {
            Place place = placesById.get(stops.get(i).placeId());
            if (place == null || place.latitude() == null || place.longitude() == null) {
                withoutLocation++;
                continue;
            }
            pins.add(new ItineraryMap.Stop(i + 1, stops.get(i).placeName(), place.latitude(), place.longitude()));
        }
        map.show(pins, withoutLocation);

        mapSubtitle.setText(stops.isEmpty()
                ? "Add places to this day to see them on the map."
                : "Follow your places in the order you'll visit them.");
        mapSummaryTitle.setText(stops.isEmpty() ? "Nothing planned yet."
                : stops.size() == 1 ? "1 place. One memorable day."
                : Trips.plural(stops.size(), "place") + ". One memorable day.");
        firstStopLabel.setText(stops.isEmpty() ? "—" : stops.get(0).placeName());
        lastStopLabel.setText(stops.isEmpty() ? "—" : stops.get(stops.size() - 1).placeName());
    }


    // ---------------------------------------------------------------
    //  Changing the trip
    // ---------------------------------------------------------------

    private void moveStop(UserDAO.ItineraryItem stop, int newDay) {
        try {
            UserDAO.moveItemToDay(stop.itemId(), newDay);
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(stopsBox, "Couldn't move that stop.", true);
        }
        refresh();
    }

    private void removeStop(UserDAO.ItineraryItem stop) {
        try {
            UserDAO.removeItineraryItem(stop.itemId());
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(stopsBox, "Couldn't remove that stop.", true);
        }
        refresh();
    }

    @FXML
    protected void onRenameClicked() {
        Optional<String> name = Ui.askText(renameButton, "Rename trip", "Trip name:", trip.name());
        if (name.isEmpty() || name.get().isBlank()) return;
        try {
            UserDAO.renameItinerary(trip.id(), name.get());
            trip = new UserDAO.Itinerary(trip.id(), name.get(), trip.startDate(), trip.endDate());
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(renameButton, "Couldn't rename the trip.", true);
        }
        refresh();
    }

    @FXML
    protected void onDeleteTripClicked() {
        if (!Ui.confirm(deleteTripButton, "Delete \"" + trip.name() + "\" and everything planned in it?", "Delete trip")) {
            return;
        }
        try {
            UserDAO.deleteItinerary(trip.id());   // its stops are deleted automatically
            Session.setCurrentItineraryId(null);
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(deleteTripButton, "Couldn't delete the trip.", true);
            return;
        }
        Navigator.go(deleteTripButton, Navigator.DASHBOARD);
    }

    // Browse places, with "Save to my trip" adding to this trip
    @FXML
    protected void onAddPlaceClicked() {
        if (trip != null) Session.setCurrentItineraryId(trip.id());
        Navigator.go(addPlaceButton, Navigator.BROWSE);
    }

    @FXML
    protected void onBackToDashboardClicked() {
        Navigator.go(dashboardCrumb, Navigator.DASHBOARD);
    }
}