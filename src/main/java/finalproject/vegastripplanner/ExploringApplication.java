package finalproject.vegastripplanner;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class ExploringApplication {
    BorderPane base;
    String css;

    // Builds the whole Exploring page and returns it as a Scene.
    // MainController calls this so it can put the page in the SAME full-screen
    // window as the rest of the app.
    public Scene createScene(double width, double height) {
        base = new BorderPane();
        Scene scene = new Scene(base, width, height);

        // style.css sits next to the .fxml files. It supplies the background image,
        // the label colour and the button look, so nothing is styled by hand here.
        css = getClass().getResource("style.css").toExternalForm();
        scene.getStylesheets().add(css);

        layout();
        return scene;
    }

    // Only used if you ever run this page on its own, in a normal window.
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Just Exploring");
        primaryStage.setScene(createScene(1000, 600));
        primaryStage.show();
    }

    public void layout() {
        // Button labels, and the matching category name stored in the database.
        // The two arrays line up: "Hotels" -> "hotel", "Restaurants" -> "restaurant", etc.
        String[] places = {"Hotels", "Restaurants", "Entertainment", "Bars", "Shops"};
        String[] categories = {"hotel", "restaurant", "entertainment", "bar", "shopping"};

        // ---- top: title, then the row of category buttons, all centered ----
        HBox buttonRow = new HBox(20);
        buttonRow.setAlignment(Pos.CENTER);

        for (int i = 0; i < places.length; i++) {
            Button button = new Button(places[i]);
            button.setPrefSize(180, 45);

            // Copy this button's category into its own variable so the click code can use it
            String category = categories[i];

            button.setOnAction(event -> {
                Stage place = new Stage(); // New window
                place.initOwner(base.getScene().getWindow()); // keeps it on top of the full-screen page
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

        VBox top = new VBox(20, message, buttonRow);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(20));
        base.setTop(top);

        // ---- bottom: Return on the left, Quit on the right ----
        Button returnButton = new Button("Return");
        returnButton.setPrefSize(150, 45);
        returnButton.setOnAction(this::returnToMain);

        Button quitButton = new Button("Quit");
        quitButton.setPrefSize(150, 45);
        quitButton.setOnAction(event -> Platform.exit());

        Region spacer = new Region();            // empty space that pushes the two buttons apart
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottom = new HBox(returnButton, spacer, quitButton);
        bottom.setPadding(new Insets(20));
        base.setBottom(bottom);
    }

    // Same logic as the Return button in PlanningTripController.
    private void returnToMain(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-page.fxml"));
            Parent root = loader.load();

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            window.setScene(scene);

            window.setFullScreenExitHint("");
            window.setFullScreen(true);
            window.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}