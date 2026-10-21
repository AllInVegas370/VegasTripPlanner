package finalproject.vegastripplanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class PlanningTripApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("planning-trip.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 375);
        primaryStage.setTitle("Planning a Trip!");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
