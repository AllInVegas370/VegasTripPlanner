package finalproject.vegastripplanner;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;

// The whole browsing section: a list in the middle, a details panel on the right.
// BorderPane is a layout with five slots (top, bottom, left, right, center).
public class PlacesBrowser extends BorderPane {

    private final ListView<Place> listView = new ListView<>();

    private final VBox detailPanel = new VBox(8);
    private final ImageView photo = new ImageView();
    private final Label title = new Label();
    private final Label category = new Label();
    private final Label price = new Label();
    private final Label location = new Label();
    private final Label description = new Label();

    // categoryFilter = "hotel", "restaurant", "bar", "entertainment" or "shopping".
    // Pass null to show every place.
    public PlacesBrowser(String categoryFilter) {
        // ---- list of places ----
        List<Place> places = new PlaceDao("AllInVegas.db").getAllPlaces();
        if (categoryFilter != null) {
            // keep only the places whose category matches the button that was clicked
            places = places.stream()
                    .filter(p -> categoryFilter.equalsIgnoreCase(p.category()))
                    .toList();
        }
        listView.setItems(FXCollections.observableArrayList(places));

        // Each row shows the name and, on a second line, the category.
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Place p, boolean empty) {
                super.updateItem(p, empty);   // required: JavaFX reuses row objects
                setText(empty || p == null ? null : p.name() + "\n" + p.categoryDisplay());
            }
        });
        setCenter(listView);

        // ---- details panel ----
        photo.setFitWidth(280);
        photo.setPreserveRatio(true);
        // The database has no photos yet, so use one of your existing images as a stand-in.
        URL img = PlacesBrowser.class.getResource("Images/vegas-landscape.jpg");
        if (img != null) photo.setImage(new Image(img.toExternalForm()));

        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        for (Label l : new Label[]{title, location, description}) l.setWrapText(true);

        Button close = new Button("Close");
        close.setOnAction(e -> listView.getSelectionModel().clearSelection());

        detailPanel.getChildren().addAll(close, photo, title, category, price, location, description);
        detailPanel.setPadding(new Insets(12));
        detailPanel.setPrefWidth(320);

        // ---- show/hide the panel whenever the selected row changes ----
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldPlace, p) -> {
            if (p == null) {
                setRight(null);               // nothing selected: remove the panel
            } else {
                title.setText(p.name());
                category.setText("Category: " + p.categoryDisplay());
                price.setText("Price: " + p.priceDisplay());
                location.setText("Location: " + p.locationDisplay());
                description.setText("Description: coming soon");   // no description column yet
                setRight(detailPanel);
            }
        });
    }
}