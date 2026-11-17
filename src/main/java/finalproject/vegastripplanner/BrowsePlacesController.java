package finalproject.vegastripplanner;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.*;

public class BrowsePlacesController {

    private static final int PAGE_SIZE = 8;

    @FXML private HBox sortBar;
    @FXML private Pagination pagination;

    private final List<Place> allPlaces = new ArrayList<>();
    private List<Place> displayedPlaces = new ArrayList<>();

    private final Set<Integer> favoriteIds = new HashSet<>();
    private final Set<Integer> itineraryIds = new HashSet<>();

    // To add a new sort parameter later, add one line here.
    private final List<SortOption> sortOptions = List.of(
            new SortOption("Category", Comparator.comparing(Place::category)),
            new SortOption("Price", Comparator.comparing(Place::priceLevel,
                    Comparator.nullsLast(Comparator.naturalOrder()))),
            new SortOption("Name", Comparator.comparing(Place::name))
    );

    private SortOption activeSort = sortOptions.get(0);
    private boolean ascending = true;

    @FXML
    public void initialize() {
        allPlaces.addAll(loadPlacesFromDatabase());
        buildSortButtons();
        applySort();
    }

    // ---- sorting ----

    private void buildSortButtons() {
        for (SortOption option : sortOptions) {
            Button button = new Button(option.label());
            button.setOnAction(e -> {
                if (option == activeSort) {
                    ascending = !ascending;   // same button again flips the direction
                } else {
                    activeSort = option;
                    ascending = true;
                }
                applySort();
                updateButtonLabels();
            });
            sortBar.getChildren().add(button);
        }
        updateButtonLabels();
    }

    private void updateButtonLabels() {
        for (int i = 0; i < sortOptions.size(); i++) {
            Button button = (Button) sortBar.getChildren().get(i + 1); // +1 skips the "Sort by:" label
            SortOption option = sortOptions.get(i);
            String arrow = (option == activeSort) ? (ascending ? " ▲" : " ▼") : "";
            button.setText(option.label() + arrow);
        }
    }

    private void applySort() {
        Comparator<Place> comparator = activeSort.comparator();
        if (!ascending) comparator = comparator.reversed();

        displayedPlaces = new ArrayList<>(allPlaces);
        displayedPlaces.sort(comparator);

        int pageCount = Math.max(1, (int) Math.ceil(displayedPlaces.size() / (double) PAGE_SIZE));
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        pagination.setPageFactory(this::createPage);   // setting it again rebuilds the current page
    }

    // ---- pages and rows ----

    private VBox createPage(int pageIndex) {
        int from = pageIndex * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, displayedPlaces.size());

        VBox page = new VBox(6);
        page.setPadding(new Insets(0, 12, 0, 12));
        for (Place place : displayedPlaces.subList(from, to)) {
            page.getChildren().add(createRow(place));
        }
        return page;
    }

    private HBox createRow(Place place) {
        Label name = new Label(place.name());
        name.getStyleClass().add("place-name");
        Label details = new Label(place.categoryDisplay() + "  •  " + place.priceDisplay());
        VBox text = new VBox(2, name, details);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button favorite = new Button();
        updateFavoriteButton(favorite, place);
        favorite.setOnAction(e -> {
            toggle(favoriteIds, place.id());
            updateFavoriteButton(favorite, place);
        });

        Button itinerary = new Button();
        updateItineraryButton(itinerary, place);
        itinerary.setOnAction(e -> {
            toggle(itineraryIds, place.id());
            updateItineraryButton(itinerary, place);
        });

        HBox row = new HBox(10, text, spacer, favorite, itinerary);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.getStyleClass().add("place-row");
        row.setOnMouseClicked(e -> showDetails(place));   // opens your existing side panel
        return row;
    }

    private void toggle(Set<Integer> ids, int id) {
        if (!ids.remove(id)) ids.add(id);
    }

    private void updateFavoriteButton(Button b, Place p) {
        b.setText(favoriteIds.contains(p.id()) ? "★ Favorited" : "☆ Favorite");
    }

    private void updateItineraryButton(Button b, Place p) {
        b.setText(itineraryIds.contains(p.id()) ? "✓ In itinerary" : "+ Itinerary");
    }

    private void showDetails(Place place) {
        // call your existing side-panel code here
    }

    private List<Place> loadPlacesFromDatabase() {
        return new PlaceDao("AllInVegas.db").getAllPlaces();
    }
}