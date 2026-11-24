package finalproject.vegastripplanner;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The "Browse places" page: search, category and price filters, a list of places,
// and a details panel for whichever place is selected.
public class BrowsePlacesController {

    // ---- things from browse-places.fxml ----
    @FXML private NavBarController navBarController;   // the included nav-bar.fxml
    @FXML private Label greetingLabel;
    @FXML private TextField searchField;
    @FXML private FlowPane categoryChips;
    @FXML private FlowPane priceChips;
    @FXML private Label filterSummary;
    @FXML private Label countLabel;
    @FXML private ComboBox<SortOption> sortBox;
    @FXML private ListView<Place> placesList;

    @FXML private StackPane detailImageBox;
    @FXML private ImageView detailImage;
    @FXML private Label detailEyebrow;
    @FXML private Label detailTitle;
    @FXML private Label detailDescription;
    @FXML private FlowPane detailTags;
    @FXML private Label detailAddress;
    @FXML private Label tripHint;
    @FXML private Button saveButton;
    @FXML private Button favoriteButton;
    @FXML private Button directionsButton;

    private static final String ALL = "ALL";   // userData of the "Select all" chips
    private static final String CHIP_LABEL = "chipLabel";
    private static final double HERO_HEIGHT = 220;
    private static final double THUMB_WIDTH = 120;
    private static final double THUMB_HEIGHT = 86;
    private static final double ROW_HEIGHT = 118;   // card (106) + gap (12)

    // Database category name -> name shown on screen
    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "casino", "Casinos",
            "hotel", "Hotels",
            "entertainment", "Entertainment",
            "attraction", "Attractions",
            "restaurant", "Restaurants",
            "bar", "Bars",
            "shopping", "Shopping");

    // category_ID in categoryTable -> category name (the chips' userData)
    private static final Map<Integer, String> CATEGORY_BY_ID = Map.of(
            1, "restaurant", 2, "bar", 3, "hotel", 4, "entertainment",
            5, "shopping", 6, "casino", 7, "attraction");

    // ---- data and current choices ----
    private final List<Place> allPlaces = new ArrayList<>();
    private final Set<String> activeCategories = new LinkedHashSet<>();
    private final Set<String> activePrices = new LinkedHashSet<>();
    private final Set<Integer> itineraryIds = new HashSet<>();   // places in the current trip ("Save to my trip")
    private final Set<Integer> favoriteIds = new HashSet<>();    // places the user favorited
    private Image currentHeroImage;
    private String currentTripName;

    // To add another way to sort, add one line to this list.
    private final List<SortOption> sortOptions = List.of(
            new SortOption("Name (A–Z)",
                    Comparator.comparing(Place::name, String.CASE_INSENSITIVE_ORDER)),
            new SortOption("Name (Z–A)",
                    Comparator.comparing(Place::name, String.CASE_INSENSITIVE_ORDER).reversed()),
            new SortOption("Category",
                    Comparator.comparing(BrowsePlacesController::categoryLabel)
                            .thenComparing(Place::name, String.CASE_INSENSITIVE_ORDER)),
            new SortOption("Price (low to high)",
                    Comparator.comparing(BrowsePlacesController::priceRank,
                                    Comparator.nullsLast(Comparator.<Integer>naturalOrder()))
                            .thenComparing(Place::name, String.CASE_INSENSITIVE_ORDER)),
            new SortOption("Price (high to low)",
                    Comparator.comparing(BrowsePlacesController::priceRank,
                                    Comparator.nullsLast(Comparator.<Integer>reverseOrder()))
                            .thenComparing(Place::name, String.CASE_INSENSITIVE_ORDER))
    );


    // ---------------------------------------------------------------
    //  Start-up
    // ---------------------------------------------------------------

    @FXML
    public void initialize() {
        navBarController.setActive(NavBarController.BROWSE);
        if (Session.isLoggedIn()) {
            greetingLabel.setText("GOOD TO SEE YOU, " + Session.getUser().firstName().toUpperCase() + ".");
        }

        // Load the places, skipping any row that has no name.
        for (Place p : new PlaceDao().getAllPlaces()) {
            if (p.name() != null && !p.name().isBlank()) {
                allPlaces.add(p);
            }
        }

        setUpChips(categoryChips, activeCategories);
        setUpChips(priceChips, activePrices);
        loadUserData();
        setUpSortBox();
        setUpList();
        setUpDetailImage();

        // Filter the list as the person types
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());

        applyFilters();
    }

    // For a signed-in user: load their favorites and current trip,
    // and start the category chips on the interests they picked on the Plan a Trip page.
    private void loadUserData() {
        if (!Session.isLoggedIn()) return;
        int userId = Session.getUser().id();

        try {
            for (UserDAO.FavoritePlace f : UserDAO.getFavorites(userId)) {
                favoriteIds.add(f.placeId());
            }

            // Only look up a trip if they have one; a new one is made on their first "Save to my trip"
            if (Session.getCurrentItineraryId() != null || !UserDAO.getItineraries(userId).isEmpty()) {
                UserDAO.Itinerary trip = UserDAO.getOrCreateCurrentItinerary(userId);
                itineraryIds.addAll(UserDAO.getPlaceIdsInItinerary(trip.id()));
                currentTripName = trip.name();
            }

            List<String> interests = new ArrayList<>();
            for (int categoryId : UserDAO.getInterests(userId)) {
                String name = CATEGORY_BY_ID.get(categoryId);
                if (name != null && chipKeys(categoryChips).contains(name)) interests.add(name);
            }
            if (!interests.isEmpty()) {
                activeCategories.clear();
                activeCategories.addAll(interests);
                refreshChips(categoryChips, activeCategories);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setUpSortBox() {
        sortBox.setItems(FXCollections.observableArrayList(sortOptions));
        sortBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SortOption option) {
                return option == null ? "" : "Sort: " + option.label();
            }

            @Override
            public SortOption fromString(String text) {
                return null;   // the box is not editable, so this is never used
            }
        });
        sortBox.getSelectionModel().selectFirst();
        sortBox.setOnAction(e -> applyFilters());
    }

    private void setUpList() {
        placesList.setFixedCellSize(ROW_HEIGHT);
        placesList.setCellFactory(lv -> new PlaceCell());
        placesList.setPlaceholder(new Label("No places match your search and filters."));
        placesList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldPlace, newPlace) -> showDetails(newPlace));
    }

    // Rounds the top corners of the big photo and keeps it filled when the panel is resized.
    private void setUpDetailImage() {
        Rectangle clip = new Rectangle();
        // The clip is taller than the box, so only the top corners show as rounded.
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        detailImageBox.layoutBoundsProperty().addListener((obs, oldBounds, bounds) -> {
            clip.setWidth(bounds.getWidth());
            clip.setHeight(bounds.getHeight() + 20);
            Ui.showCover(detailImage, currentHeroImage, bounds.getWidth(), HERO_HEIGHT);
        });
        detailImageBox.setClip(clip);
    }


    // ---------------------------------------------------------------
    //  Filter chips (categories and price levels)
    // ---------------------------------------------------------------

    private void setUpChips(FlowPane box, Set<String> active) {
        for (Node node : box.getChildren()) {
            ToggleButton chip = (ToggleButton) node;
            chip.getProperties().put(CHIP_LABEL, chip.getText());   // remember the text without the check mark
            if (!ALL.equals(chip.getUserData())) {
                active.add((String) chip.getUserData());            // everything starts switched on
            }
            chip.setOnAction(e -> onChipClicked(box, active, chip));
        }
        refreshChips(box, active);
    }

    private void onChipClicked(FlowPane box, Set<String> active, ToggleButton clicked) {
        String key = (String) clicked.getUserData();
        List<String> allKeys = chipKeys(box);

        if (ALL.equals(key)) {
            // "Select all" always switches everything on
            active.clear();
            active.addAll(allKeys);
        } else if (active.size() == allKeys.size()) {
            // Everything was on, so clicking one chip narrows the list to just that one
            active.clear();
            active.add(key);
        } else if (active.contains(key)) {
            active.remove(key);
            if (active.isEmpty()) {
                active.addAll(allKeys);   // never leave nothing selected
            }
        } else {
            active.add(key);
        }

        refreshChips(box, active);
        applyFilters();
    }

    // The values of every chip except "Select all"
    private List<String> chipKeys(FlowPane box) {
        List<String> keys = new ArrayList<>();
        for (Node node : box.getChildren()) {
            String key = (String) node.getUserData();
            if (!ALL.equals(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    // Makes each chip look on or off to match the current choices
    private void refreshChips(FlowPane box, Set<String> active) {
        int total = chipKeys(box).size();
        for (Node node : box.getChildren()) {
            ToggleButton chip = (ToggleButton) node;
            String key = (String) chip.getUserData();
            boolean on = ALL.equals(key) ? active.size() == total : active.contains(key);
            chip.setSelected(on);
            String label = (String) chip.getProperties().get(CHIP_LABEL);
            chip.setText(on ? "✓ " + label : label);
        }
    }


    // ---------------------------------------------------------------
    //  Filtering and sorting
    // ---------------------------------------------------------------

    @FXML
    protected void onSearchClicked() {
        applyFilters();
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        boolean filterByPrice = activePrices.size() < chipKeys(priceChips).size();
        SortOption sort = sortBox.getValue() != null ? sortBox.getValue() : sortOptions.get(0);

        List<Place> results = new ArrayList<>();
        for (Place p : allPlaces) {
            if (!activeCategories.contains(p.category())) continue;

            // With every price level switched on, places with no price still show.
            // Once the person narrows the price levels, only places with a matching price show.
            if (filterByPrice && (p.priceLevel() == null || !activePrices.contains(p.priceLevel()))) continue;

            if (!search.isEmpty() && !matchesSearch(p, search)) continue;

            results.add(p);
        }
        results.sort(sort.comparator());

        Place previous = placesList.getSelectionModel().getSelectedItem();
        placesList.getSelectionModel().clearSelection();
        placesList.getItems().setAll(results);

        countLabel.setText(results.size() == 1 ? "1 place" : results.size() + " places");
        filterSummary.setText(describe(activeCategories, chipKeys(categoryChips).size(), true)
                + " · " + describe(activePrices, chipKeys(priceChips).size(), false));

        // Keep the same place selected if it is still in the list, otherwise pick the first one
        if (previous != null && results.contains(previous)) {
            placesList.getSelectionModel().select(previous);
            placesList.scrollTo(previous);
        } else if (!results.isEmpty()) {
            placesList.getSelectionModel().selectFirst();
            placesList.scrollTo(0);
        } else {
            showDetails(null);
        }
    }

    private boolean matchesSearch(Place p, String search) {
        return p.name().toLowerCase().contains(search)
                || categoryLabel(p).toLowerCase().contains(search)
                || (p.address() != null && p.address().toLowerCase().contains(search));
    }

    // Short text for the summary next to the price chips, e.g. "Hotels, Bars" or "All prices"
    private String describe(Set<String> active, int total, boolean categories) {
        if (active.size() == total) {
            return categories ? "All categories" : "All prices";
        }
        if (active.size() <= 2) {
            List<String> names = new ArrayList<>();
            for (String key : active) {
                names.add(categories ? CATEGORY_LABELS.getOrDefault(key, key) : key);
            }
            return String.join(", ", names);
        }
        return active.size() + (categories ? " categories" : " price levels");
    }

    private static String categoryLabel(Place p) {
        return CATEGORY_LABELS.getOrDefault(p.category(), p.categoryDisplay());
    }

    // Free = 0, $ = 1, $$ = 2, $$$ = 3, and null when the place has no price
    private static Integer priceRank(Place p) {
        if (p.priceLevel() == null) return null;
        return switch (p.priceLevel()) {
            case "Free" -> 0;
            case "$" -> 1;
            case "$$" -> 2;
            case "$$$" -> 3;
            default -> null;
        };
    }


    // ---------------------------------------------------------------
    //  One row in the list
    // ---------------------------------------------------------------

    private class PlaceCell extends ListCell<Place> {
        private final ImageView thumb = new ImageView();
        private final Label name = new Label();
        private final Label subtitle = new Label();
        private final Label meta = new Label();
        private final Label price = new Label();
        private final Label viewDetails = new Label("View details");
        private final HBox card;

        PlaceCell() {
            Ui.roundCorners(thumb, THUMB_WIDTH, THUMB_HEIGHT, 5);

            name.getStyleClass().add("browse-place-name");
            name.setMaxWidth(Double.MAX_VALUE);
            subtitle.getStyleClass().add("browse-place-text");
            meta.getStyleClass().add("browse-place-text");
            price.getStyleClass().add("aiv-pill");
            viewDetails.getStyleClass().add("browse-place-view");

            VBox text = new VBox(7, name, subtitle, meta);
            text.setAlignment(Pos.CENTER_LEFT);
            text.setMinWidth(0);
            HBox.setHgrow(text, Priority.ALWAYS);

            Region spacer = new Region();
            VBox.setVgrow(spacer, Priority.ALWAYS);
            VBox right = new VBox(price, spacer, viewDetails);
            right.setAlignment(Pos.TOP_RIGHT);
            right.setMinWidth(100);
            right.setPadding(new javafx.geometry.Insets(4, 4, 4, 0));

            card = new HBox(18, thumb, text, right);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("browse-place");
            card.setMinHeight(106);
            card.setMaxHeight(106);   // leaves a gap between rows (each row is 118 high)
            // A cell does not stretch its contents by itself, so tie the card's width to the row's width
            card.prefWidthProperty().bind(widthProperty().subtract(14));

            setGraphic(null);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setAlignment(Pos.TOP_LEFT);

            // The selected row says "Viewing details" instead of "View details"
            selectedProperty().addListener((obs, was, isSelected) ->
                    viewDetails.setText(isSelected ? "Viewing details" : "View details"));
        }

        @Override
        protected void updateItem(Place place, boolean empty) {
            super.updateItem(place, empty);   // required: JavaFX reuses row objects
            if (empty || place == null) {
                setGraphic(null);
                return;
            }

            Ui.showCover(thumb, Ui.placeImage(place.id(), 240), THUMB_WIDTH, THUMB_HEIGHT);
            name.setText(place.name());

            String shortAddress = Ui.shortAddress(place.address());
            subtitle.setText(shortAddress);
            subtitle.setVisible(!shortAddress.isEmpty());
            subtitle.setManaged(!shortAddress.isEmpty());

            String extras = "";
            if (favoriteIds.contains(place.id())) extras += "  ·  ♥ Favorite";
            if (itineraryIds.contains(place.id())) extras += "  ·  ✓ In your trip";
            meta.setText(categoryLabel(place) + extras);

            boolean hasPrice = place.priceLevel() != null && !place.priceLevel().isBlank();
            price.setText(hasPrice ? place.priceLevel() : "");
            price.setVisible(hasPrice);
            price.setManaged(hasPrice);

            viewDetails.setText(isSelected() ? "Viewing details" : "View details");
            setGraphic(card);
        }
    }

    // ---------------------------------------------------------------
    //  Details panel
    // ---------------------------------------------------------------

    private void showDetails(Place place) {
        detailTags.getChildren().clear();
        if (place == null) {
            detailEyebrow.setText("");
            detailTitle.setText("No place selected");
            detailDescription.setText("Try changing your search or filters to see places here.");
            detailAddress.setText("—");
            setHeroImage(null);
            saveButton.setDisable(true);
            favoriteButton.setDisable(true);
            directionsButton.setDisable(true);
            return;
        }

        boolean hasPrice = place.priceLevel() != null && !place.priceLevel().isBlank();
        detailEyebrow.setText(categoryLabel(place).toUpperCase() + (hasPrice ? "  /  " + place.priceLevel() : ""));
        detailTitle.setText(place.name());
        // The database has no descriptions yet. When a description column is added, show it here.
        detailDescription.setText("More details about this place are coming soon. "
                + "Save it to your trip or favorites to keep it close.");
        detailAddress.setText(place.locationDisplay().equals("Not listed") ? "Address not listed" : place.locationDisplay());
        setHeroImage(Ui.placeImage(place.id(), 960));

        saveButton.setDisable(false);
        favoriteButton.setDisable(false);
        directionsButton.setDisable(false);
        updateSaveButton(place);
        updateFavoriteButton(place);
        updateTags(place);
    }

    // Little rounded tags under the description
    private void updateTags(Place place) {
        detailTags.getChildren().clear();
        List<String> tags = new ArrayList<>();
        tags.add(categoryLabel(place));
        boolean hasPrice = place.priceLevel() != null && !place.priceLevel().isBlank();
        tags.add(!hasPrice ? "Price not listed" : place.priceLevel().equals("Free") ? "Free admission" : place.priceLevel());
        if (favoriteIds.contains(place.id())) tags.add("♥ In favorites");
        if (itineraryIds.contains(place.id())) tags.add("✓ In your trip");
        for (String tag : tags) {
            Label label = new Label(tag);
            label.getStyleClass().add("aiv-tag");
            detailTags.getChildren().add(label);
        }
    }

    private void setHeroImage(Image image) {
        currentHeroImage = image;
        double width = detailImageBox.getWidth() > 0 ? detailImageBox.getWidth() : 470;
        Ui.showCover(detailImage, image, width, HERO_HEIGHT);
    }

    private void updateSaveButton(Place place) {
        saveButton.setText(itineraryIds.contains(place.id()) ? "✓ Saved to my trip" : "Save to my trip");
        if (!Session.isLoggedIn()) {
            tripHint.setText("Sign in to save places to a trip and keep favorites.");
        } else if (currentTripName != null) {
            tripHint.setText("Saving adds this place to day 1 of \"" + currentTripName + "\". Plan the days on your itinerary.");
        } else {
            tripHint.setText("Saving starts a new trip for you. Plan the days on your itinerary.");
        }
    }

    private void updateFavoriteButton(Place place) {
        favoriteButton.setText(favoriteIds.contains(place.id()) ? "♥  In your favorites" : "♡  Add to favorites");
    }

    // Shows changes (saved / favorited) on the selected row and in the tags
    private void refreshSelected(Place place) {
        updateSaveButton(place);
        updateFavoriteButton(place);
        updateTags(place);
        placesList.refresh();
    }

    // Adds the place to the current trip (day 1), or takes it out if it's already there.
    // Days can be changed on the itinerary page.
    @FXML
    protected void onSaveClicked() {
        Place place = placesList.getSelectionModel().getSelectedItem();
        if (place == null) return;
        if (!Session.isLoggedIn()) {
            showMessage("Sign in to save places to a trip.");
            return;
        }

        try {
            UserDAO.Itinerary trip = UserDAO.getOrCreateCurrentItinerary(Session.getUser().id());
            currentTripName = trip.name();
            if (itineraryIds.contains(place.id())) {
                UserDAO.removePlaceFromItinerary(trip.id(), place.id());
                itineraryIds.remove(place.id());
            } else {
                UserDAO.addPlaceToItinerary(trip.id(), place.id(), 1);
                itineraryIds.add(place.id());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Couldn't update your trip. Please try again.");
        }
        refreshSelected(place);
    }

    @FXML
    protected void onFavoriteClicked() {
        Place place = placesList.getSelectionModel().getSelectedItem();
        if (place == null) return;
        if (!Session.isLoggedIn()) {
            showMessage("Sign in to save favorites.");
            return;
        }

        int userId = Session.getUser().id();
        try {
            if (favoriteIds.contains(place.id())) {
                UserDAO.removeFavorite(userId, place.id());
                favoriteIds.remove(place.id());
            } else {
                UserDAO.addFavorite(userId, place.id());
                favoriteIds.add(place.id());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Couldn't update your favorites. Please try again.");
        }
        refreshSelected(place);
    }

    // Opens Google Maps directions to the selected place in the computer's web browser
    @FXML
    protected void onDirectionsClicked() {
        Place place = placesList.getSelectionModel().getSelectedItem();
        if (place == null) return;

        String destination;
        if (place.address() != null && !place.address().isBlank()) {
            destination = place.name() + ", " + place.address();
        } else if (place.latitude() != null && place.longitude() != null) {
            destination = place.latitude() + "," + place.longitude();
        } else {
            destination = place.name() + ", Las Vegas, NV";
        }
        String url = "https://www.google.com/maps/dir/?api=1&destination="
                + URLEncoder.encode(destination, StandardCharsets.UTF_8);

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            } else {
                showMessage("Couldn't open a web browser on this computer.");
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            showMessage("Couldn't open a web browser on this computer.");
        }
    }


    // ---------------------------------------------------------------
    //  Opening a specific place (used by the itinerary page's "View place")
    // ---------------------------------------------------------------

    /** Clears the search and filters, then selects and scrolls to this place. */
    public void showPlace(int placeId) {
        searchField.setText("");
        activeCategories.clear();
        activeCategories.addAll(chipKeys(categoryChips));
        activePrices.clear();
        activePrices.addAll(chipKeys(priceChips));
        refreshChips(categoryChips, activeCategories);
        refreshChips(priceChips, activePrices);
        applyFilters();

        for (Place p : placesList.getItems()) {
            if (p.id() == placeId) {
                placesList.getSelectionModel().select(p);
                placesList.scrollTo(p);
                return;
            }
        }
    }

    private void showMessage(String message) {
        Ui.info(placesList, message);
    }
}