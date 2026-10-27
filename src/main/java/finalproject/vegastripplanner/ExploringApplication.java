package finalproject.vegastripplanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;

public class ExploringApplication{
    Pane base;

    public void start(Stage primaryStage) {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        base = new Pane();
        Scene scene = new Scene(base, 1000, 600);
        boolean justExploring = true;
        layout();

        primaryStage.setTitle("Just Exploring");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public void layout() {
        //On action, it should show access for
        //Could make a for loop to create the buttons instead of manually creating them
        //Resize buttons and arrange location of buttons
        String[] places = {"Hotels", "Restaurants", "Entertainment", "Attractions", "Shops"};

        Image background = new Image("file:src/main/Images/VegasBackDrop.jpg");
        ImageView imageView = new ImageView(background);
        imageView.setFitWidth(1000);
        imageView.setFitHeight(600);
        imageView.setOpacity(0.9);
        //imageView.setPreserveRatio(true);
        base.getChildren().add(imageView);

        for(int i = 0; i < places.length; i++) {
            Button button = new Button(places[i]);
            button.setLayoutX(100 + i * 160);
            button.setLayoutY(20);
            button.setPrefSize(150, 30);
            button.setOnAction(event -> {
                Stage place = new Stage(); //New Window
                String placeName = button.getText();
                place.setTitle(placeName);

                Pane newPane = new Pane();

                Scene newScene = new Scene(newPane, 500, 300);
                place.setTitle(placeName);
                place.setScene(newScene);
                place.show();
            });
            base.getChildren().add(button);
        }

        Label message = new Label("Welcome to AllInVegas!");
        message.setLayoutX(350);
        message.setLayoutY(100);
        message.setFont(Font.font(30));
        message.setStyle("-fx-text-fill: white;");
        base.getChildren().add(message);

    }
}
