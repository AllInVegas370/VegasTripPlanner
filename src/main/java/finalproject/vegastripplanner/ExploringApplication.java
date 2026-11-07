package finalproject.vegastripplanner;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class ExploringApplication {
    BorderPane base;
    String css;

    public void start(Stage primaryStage) {
        FXMLLoader fxmlLoader = new FXMLLoader(MainPageApplication.class.getResource("exploring.fxml"));
        base = new BorderPane();
        Scene scene = new Scene(base, 1000, 600);
        boolean justExploring = true;

        // style.css sits next to the .fxml files. It supplies the background image,
        // the label colour and the button look, so nothing is styled by hand here.
        css = getClass().getResource("style.css").toExternalForm();
        scene.getStylesheets().add(css);

        layout();

        primaryStage.setTitle("Just Exploring");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void layout() {
        // Button labels, and the matching category name stored in the database.
        // The two arrays line up: "Hotels" -> "hotel", "Restaurants" -> "restaurant", etc.
        String[] places = {"Hotels", "Restaurants", "Entertainment", "Bars", "Shops"};
        String[] categories = {"hotel", "restaurant", "entertainment", "bar", "shopping"};

        // Row of buttons, centered
        HBox buttonRow = new HBox(20);
        buttonRow.setAlignment(Pos.CENTER);

        for (int i = 0; i < places.length; i++) {
            Button button = new Button(places[i]);
            button.setPrefSize(180, 45);

            // Copy this button's category into its own variable so the click code can use it
            String category = categories[i];

            button.setOnAction(event -> {
                Stage place = new Stage(); // New window
                place.setTitle(button.getText());

                // The list of places for this category, with the side panel on click
                Scene newScene = new Scene(new PlacesBrowser(category), 800, 500);
                newScene.getStylesheets().add(css);
                place.setScene(newScene);
                place.show();
            });
            buttonRow.getChildren().add(button);
        }

        Label message = new Label("Welcome to AllInVegas!");
        message.setFont(Font.font(30));

        // Title on top, buttons underneath, all centered at the top of the page
        VBox top = new VBox(20, message, buttonRow);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(20));

        base.setTop(top);
    }
}