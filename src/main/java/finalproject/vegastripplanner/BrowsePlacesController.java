package finalproject.vegastripplanner;

import javafx.collections.FXCollections;
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
import javafx.scene.control.ComboBox;
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
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The "Browse places" page: search, category and price filters, a list of places,
// and a details panel for whichever place is selected.
public class BrowsePlacesController {

    // ---- things from browse-places.fxml ----
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
    @FXML private Label detailAddress;
    @FXML private Button saveButton;
    @FXML private Button directionsButton;

    // ---- settings you may want to change ----
    private static final String DATABASE_FILE = "AllInVegas.db";

    // A photo for one place goes in Images/places/ and is named after the place's ID,
    // for example Images/places/164.jpg. Places without a photo use the fallback.
    private static final String PLACE_IMAGE_FOLDER = "Images/places/";
    private static final String FALLBACK_IMAGE = "Images/vegas-landscape.jpg";

    private static final String ALL = "ALL";   // userData of the "Select all" chips
    private static final String CHIP_LABEL = "chipLabel";
    private static final double HERO_HEIGHT = 190;
    private static final double THUMB_WIDTH = 100;
    private static final double THUMB_HEIGHT = 76;

    // Database category name -> name shown on screen
    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "casino", "Casinos",
            "hotel", "Hotels",
            "entertainment", "Entertainment",
            "attraction", "Attractions",
            "restaurant", "Restaurants",
            "bar", "Bars",
            "shopping", "Shopping");

    // ---- data and current choices ----
    private final List<Place> allPlaces = new ArrayList<>();
    private final Set<String> activeCategories = new LinkedHashSet<>();
    private final Set<String> activePrices = new LinkedHashSet<>();
    private final Set<Integer> itineraryIds = new HashSet<>();   // places saved with "Save to my trip"
    private final Map<String, Image> imageCache = new HashMap<>();
    private Image currentHeroImage;

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
        // Load the places, skipping any row that has no name.
        for (Place p : new PlaceDao(DATABASE_FILE).getAllPlaces()) {
            if (p.name() != null && !p.name().isBlank()) {
                allPlaces.add(p);
            }
        }

        setUpChips(categoryChips, activeCategories);
        setUpChips(priceChips, activePrices);
        setUpSortBox();
        setUpList();
        setUpDetailImage();

        // Filter the list as the person types
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());

        applyFilters();
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
        placesList.setFixedCellSize(100);
        placesList.setCellFactory(lv -> new PlaceCell());
        placesList.setPlaceholder(new Label("No places match your search and filters."));
        placesList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldPlace, newPlace) -> showDetails(newPlace));
    }

    // Rounds the top corners of the big photo and keeps it filled when the panel is resized.
    private void setUpDetailImage() {
        Rectangle clip = new Rectangle();
        // The clip is taller than the box, so only the top corners show as rounded.
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        detailImageBox.layoutBoundsProperty().addListener((obs, oldBounds, bounds) -> {
            clip.setWidth(bounds.getWidth());
            clip.setHeight(bounds.getHeight() + 20);
            showCover(detailImage, currentHeroImage, bounds.getWidth(), HERO_HEIGHT);
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
            Rectangle clip = new Rectangle(THUMB_WIDTH, THUMB_HEIGHT);
            clip.setArcWidth(12);
            clip.setArcHeight(12);
            thumb.setClip(clip);

            name.getStyleClass().add("place-name");
            name.setMaxWidth(Double.MAX_VALUE);
            subtitle.getStyleClass().add("place-subtitle");
            meta.getStyleClass().add("place-meta");
            price.getStyleClass().add("price-pill");
            viewDetails.getStyleClass().add("place-view");

            VBox text = new VBox(4, name, subtitle, meta);
            text.setAlignment(Pos.CENTER_LEFT);
            text.setMinWidth(0);
            HBox.setHgrow(text, Priority.ALWAYS);

            Region spacer = new Region();
            VBox.setVgrow(spacer, Priority.ALWAYS);
            VBox right = new VBox(price, spacer, viewDetails);
            right.setAlignment(Pos.TOP_RIGHT);
            right.setPrefWidth(110);
            right.setMinWidth(110);

            card = new HBox(16, thumb, text, right);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("place-card");
            card.setMaxHeight(92);   // leaves a gap between rows (each row is 100 high)
            // A cell does not stretch its contents by itself, so tie the card's width to the row's width
            card.prefWidthProperty().bind(widthProperty().subtract(12));

            setGraphic(null);
            setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
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

            showCover(thumb, loadImage(place, 240), THUMB_WIDTH, THUMB_HEIGHT);
            name.setText(place.name());

            String shortAddress = shortAddress(place);
            subtitle.setText(shortAddress);
            subtitle.setVisible(!shortAddress.isEmpty());
            subtitle.setManaged(!shortAddress.isEmpty());

            meta.setText(categoryLabel(place));

            boolean hasPrice = place.priceLevel() != null && !place.priceLevel().isBlank();
            price.setText(hasPrice ? place.priceLevel() : "");
            price.setVisible(hasPrice);
            price.setManaged(hasPrice);

            viewDetails.setText(isSelected() ? "Viewing details" : "View details");
            setGraphic(card);
        }
    }

    // Just the street part of an address, e.g. "3600 South Las Vegas Boulevard"
    private static String shortAddress(Place p) {
        if (p.address() == null || p.address().isBlank()) return "";
        int comma = p.address().indexOf(',');
        return comma > 0 ? p.address().substring(0, comma) : p.address();
    }


    // ---------------------------------------------------------------
    //  Details panel
    // ---------------------------------------------------------------

    private void showDetails(Place place) {
        if (place == null) {
            detailEyebrow.setText("");
            detailTitle.setText("No place selected");
            detailDescription.setText("Try changing your search or filters to see places here.");
            detailAddress.setText("—");
            setHeroImage(null);
            saveButton.setDisable(true);
            directionsButton.setDisable(true);
            return;
        }

        boolean hasPrice = place.priceLevel() != null && !place.priceLevel().isBlank();
        detailEyebrow.setText(categoryLabel(place).toUpperCase() + (hasPrice ? "  /  " + place.priceLevel() : ""));
        detailTitle.setText(place.name());
        // The database has no descriptions yet. When a description column is added, show it here.
        detailDescription.setText("More details about this place are coming soon.");
        detailAddress.setText(place.locationDisplay().equals("Not listed") ? "Address not listed" : place.locationDisplay());
        setHeroImage(loadImage(place, 920));

        saveButton.setDisable(false);
        directionsButton.setDisable(false);
        updateSaveButton(place);
    }

    private void setHeroImage(Image image) {
        currentHeroImage = image;
        double width = detailImageBox.getWidth() > 0 ? detailImageBox.getWidth() : 450;
        showCover(detailImage, image, width, HERO_HEIGHT);
    }

    private void updateSaveButton(Place place) {
        saveButton.setText(itineraryIds.contains(place.id()) ? "✓ Saved to my trip" : "Save to my trip");
    }

    @FXML
    protected void onSaveClicked() {
        Place place = placesList.getSelectionModel().getSelectedItem();
        if (place == null) return;

        if (!itineraryIds.remove(place.id())) {
            itineraryIds.add(place.id());
        }
        updateSaveButton(place);
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
    //  Images
    // ---------------------------------------------------------------

    // Finds the photo for a place (or the fallback) and loads it at a reduced size.
    // Each file is only loaded once and then reused.
    private Image loadImage(Place place, int width) {
        URL url = getClass().getResource(PLACE_IMAGE_FOLDER + place.id() + ".jpg");
        if (url == null) {
            url = getClass().getResource(FALLBACK_IMAGE);
        }
        if (url == null) {
            return null;
        }

        String key = url + "@" + width;
        Image image = imageCache.get(key);
        if (image == null) {
            image = new Image(url.toExternalForm(), width, 0, true, true);
            imageCache.put(key, image);
        }
        return image;
    }

    // Fills a w x h box with the image, cropping the edges instead of stretching it
    private static void showCover(ImageView view, Image image, double w, double h) {
        view.setImage(image);
        view.setPreserveRatio(false);
        view.setFitWidth(w);
        view.setFitHeight(h);
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            view.setViewport(null);
            return;
        }
        double scale = Math.max(w / image.getWidth(), h / image.getHeight());
        double viewW = w / scale;
        double viewH = h / scale;
        view.setViewport(new Rectangle2D((image.getWidth() - viewW) / 2, (image.getHeight() - viewH) / 2, viewW, viewH));
    }


    // ---------------------------------------------------------------
    //  Navigation
    // ---------------------------------------------------------------

    @FXML
    protected void onLogOutClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-page.fxml"));
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

    // "My dashboard" and "Account settings" are not built yet.
    // To hook one up later, give its button its own onAction method in browse-places.fxml.
    @FXML
    protected void onComingSoonClicked() {
        showMessage("This page isn't ready yet.");
    }

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}