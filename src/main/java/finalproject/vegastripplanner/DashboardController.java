package finalproject.vegastripplanner;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// My dashboard (dashboard.fxml): quick stats, the user's trips, and their favorites.
// The trip and favorite cards are built here in code, one per row from the database.
public class DashboardController {

    private static final int TRIP_COLUMNS = 2;
    private static final int FAVORITE_COLUMNS = 3;
    private static final double TRIP_PHOTO_WIDTH = 210;
    private static final double TRIP_CARD_HEIGHT = 238;
    private static final double FAVORITE_PHOTO_HEIGHT = 148;

    @FXML private NavBarController navBarController;   // the included nav-bar.fxml
    @FXML private Label greetingLabel;
    @FXML private Label nextTripValue, nextTripDetail, tripsValue, tripsDetail, favoritesValue;
    @FXML private Label tripsHint, favoritesHint;
    @FXML private GridPane tripsGrid, favoritesGrid;
    @FXML private Button createItineraryButton, accountSettingsLink;

    private final Map<Integer, Place> placesById = new PlaceDao().getPlacesById();
    private int travelers;

    @FXML
    public void initialize() {
        navBarController.setActive(NavBarController.DASHBOARD);
        if (!Session.isLoggedIn()) {
            greetingLabel.setText("SIGN IN TO SEE YOUR TRIPS AND FAVORITES");
            return;
        }
        greetingLabel.setText("WELCOME BACK, " + Session.getUser().firstName().toUpperCase() + ". LESS PLANNING, MORE LIVING.");
        travelers = Trips.travelers(userId());
        loadTrips();
        loadFavorites();
    }

    private int userId() {
        return Session.getUser().id();
    }


    // ---------------------------------------------------------------
    //  Trips
    // ---------------------------------------------------------------

    private void loadTrips() {
        tripsGrid.getChildren().clear();
        List<UserDAO.Itinerary> trips;
        try {
            trips = UserDAO.getItineraries(userId());   // newest first
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(tripsGrid, "Couldn't load your trips.", true);
            return;
        }

        // ---- stats ----
        int upcoming = 0, drafts = 0;
        UserDAO.Itinerary next = null;
        for (UserDAO.Itinerary trip : trips) {
            if (trip.startDate() == null) drafts++;
            if (Trips.isUpcomingOrNow(trip)) {
                upcoming++;
                if (next == null || trip.startDate().isBefore(next.startDate())) next = trip;
            }
        }
        tripsValue.setText(Trips.plural(trips.size(), "itinerary").replace("itinerarys", "itineraries"));
        tripsDetail.setText(upcoming + " upcoming · " + Trips.plural(drafts, "draft"));
        if (next != null) {
            nextTripValue.setText(Trips.shortRange(next));
            nextTripDetail.setText(next.name());
        } else {
            nextTripValue.setText("Not planned yet");
            nextTripDetail.setText("Add dates to a trip");
        }
        tripsHint.setText(trips.isEmpty() ? "" : "Click a trip to plan it day by day");

        // ---- cards ----
        if (trips.isEmpty()) {
            tripsGrid.add(emptyCard("No trips yet.",
                    "Create an itinerary, then save places to it from Browse places.",
                    "+  Create itinerary", () -> onCreateItineraryClicked()), 0, 0, TRIP_COLUMNS, 1);
            return;
        }
        for (int i = 0; i < trips.size(); i++) {
            tripsGrid.add(tripCard(trips.get(i)), i % TRIP_COLUMNS, i / TRIP_COLUMNS);
        }
    }

    private HBox tripCard(UserDAO.Itinerary trip) {
        List<UserDAO.ItineraryItem> items = List.of();
        try {
            items = UserDAO.getItineraryItems(trip.id());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // photo: the first stop's photo, or the Vegas photo
        int photoPlace = items.isEmpty() ? -1 : items.get(0).placeId();
        ImageView photo = Ui.coverImage(Ui.placeImage(photoPlace, 440), TRIP_PHOTO_WIDTH, TRIP_CARD_HEIGHT);

        Label status = new Label(Trips.status(trip));
        status.getStyleClass().add("dash-trip-status");
        Label name = new Label(trip.name());
        name.getStyleClass().add("dash-trip-name");
        name.setWrapText(true);
        Label dates = new Label(Trips.dateRange(trip));
        dates.getStyleClass().add("dash-trip-dates");

        List<String> details = new ArrayList<>();
        details.add(Trips.plural(Trips.dayCount(trip, items), "day"));
        details.add(Trips.plural(items.size(), "place"));
        if (travelers > 0) details.add(Trips.plural(travelers, "traveler"));
        Label detail = new Label(String.join("  ·  ", details));
        detail.getStyleClass().add("dash-trip-details");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        // Looks like a link; the whole card is what you click
        Label open = new Label("Open itinerary  →");
        open.getStyleClass().add("aiv-lavender");
        open.setStyle("-fx-font-weight: bold;");

        VBox body = new VBox(status, name, dates, detail, spacer, open);
        body.getStyleClass().add("dash-trip-body");
        body.setMinWidth(0);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox card = new HBox(photo, body);
        card.getStyleClass().addAll("aiv-card", "aiv-card-clickable", "dash-trip");
        card.setMinHeight(TRIP_CARD_HEIGHT);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseClicked(e -> openTrip(trip));
        Ui.roundCorners(card, 8);
        return card;
    }

    private void openTrip(UserDAO.Itinerary trip) {
        Session.setCurrentItineraryId(trip.id());
        Navigator.go(tripsGrid, Navigator.ITINERARY);
    }

    // "+ Create itinerary" goes through the trip preferences page, which asks for dates
    // and starts a new trip when you click "Find my Vegas".
    @FXML
    protected void onCreateItineraryClicked() {
        Navigator.go(createItineraryButton, Navigator.PREFERENCES);
    }


    // ---------------------------------------------------------------
    //  Favorites
    // ---------------------------------------------------------------

    private void loadFavorites() {
        favoritesGrid.getChildren().clear();
        List<UserDAO.FavoritePlace> favorites;
        try {
            favorites = UserDAO.getFavorites(userId());
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(favoritesGrid, "Couldn't load your favorites.", true);
            return;
        }

        favoritesValue.setText(Trips.plural(favorites.size(), "favorite place"));
        favoritesHint.setText(favorites.isEmpty() ? ""
                : Trips.plural(favorites.size(), "saved place") + "  ·  Keep the good ideas close.");

        if (favorites.isEmpty()) {
            favoritesGrid.add(emptyCard("No favorites yet.",
                    "Tap ♡ Add to favorites on any place while you browse.",
                    "Browse places  →", () -> Navigator.go(favoritesGrid, Navigator.BROWSE)), 0, 0, FAVORITE_COLUMNS, 1);
            return;
        }
        for (int i = 0; i < favorites.size(); i++) {
            favoritesGrid.add(favoriteCard(favorites.get(i)), i % FAVORITE_COLUMNS, i / FAVORITE_COLUMNS);
        }
    }

    private VBox favoriteCard(UserDAO.FavoritePlace fav) {
        Place place = placesById.get(fav.placeId());

        StackPane photo = Ui.coverBox(Ui.placeImage(fav.placeId(), 640), FAVORITE_PHOTO_HEIGHT);

        String where = place == null ? "" : Ui.shortAddress(place.address());
        Label category = new Label((Ui.categoryLabel(fav.category()) + (where.isEmpty() ? "" : "  /  " + where)).toUpperCase());
        category.getStyleClass().add("dash-fav-category");
        category.setMinWidth(0);
        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);
        Label price = new Label(place == null || place.priceLevel() == null ? "" : place.priceLevel());
        price.getStyleClass().add("dash-fav-price");
        price.setMinWidth(Region.USE_PREF_SIZE);
        HBox meta = new HBox(12, category, metaSpacer, price);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(fav.placeName());
        name.getStyleClass().add("dash-fav-name");
        name.setWrapText(true);

        Button add = new Button("Add to itinerary  +");
        add.getStyleClass().add("aiv-link");
        add.setOnAction(e -> addFavoriteToTrip(fav));
        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        Button saved = new Button("♥ Saved");
        saved.getStyleClass().add("dash-saved");
        saved.setOnAction(e -> removeFavorite(fav));
        saved.hoverProperty().addListener((obs, was, hovering) -> saved.setText(hovering ? "Remove" : "♥ Saved"));
        HBox actions = new HBox(12, add, actionSpacer, saved);
        actions.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox body = new VBox(meta, name, spacer, actions);
        body.getStyleClass().add("dash-fav-body");

        VBox card = new VBox(photo, body);
        card.getStyleClass().add("aiv-card");
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        Ui.roundCorners(card, 8);
        return card;
    }

    // Adds a favorite to day 1 of the current trip (the one "Save to my trip" uses)
    private void addFavoriteToTrip(UserDAO.FavoritePlace fav) {
        try {
            UserDAO.Itinerary trip = UserDAO.getOrCreateCurrentItinerary(userId());
            if (UserDAO.getPlaceIdsInItinerary(trip.id()).contains(fav.placeId())) {
                Ui.toast(favoritesGrid, fav.placeName() + " is already in \"" + trip.name() + "\".", false);
                return;
            }
            UserDAO.addPlaceToItinerary(trip.id(), fav.placeId(), 1);
            Ui.toast(favoritesGrid, "Added " + fav.placeName() + " to day 1 of \"" + trip.name() + "\".", false);
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(favoritesGrid, "Couldn't add that place.", true);
            return;
        }
        loadTrips();   // place counts changed
    }

    private void removeFavorite(UserDAO.FavoritePlace fav) {
        try {
            UserDAO.removeFavorite(userId(), fav.placeId());
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(favoritesGrid, "Couldn't remove that favorite.", true);
        }
        loadFavorites();
    }


    // ---------------------------------------------------------------
    //  Shared bits
    // ---------------------------------------------------------------

    // A friendly placeholder card when a section has nothing in it yet
    private VBox emptyCard(String title, String text, String buttonText, Runnable action) {
        Label heading = new Label(title);
        heading.getStyleClass().add("aiv-card-title");
        Label body = new Label(text);
        body.getStyleClass().add("aiv-muted");
        body.setWrapText(true);
        Button button = new Button(buttonText);
        button.getStyleClass().add("aiv-button");
        button.setOnAction(e -> action.run());

        VBox card = new VBox(heading, body, button);
        card.getStyleClass().addAll("aiv-card", "dash-empty");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    @FXML
    protected void onAccountSettingsClicked() {
        Navigator.go(accountSettingsLink, Navigator.ACCOUNT_SETTINGS);
    }
}