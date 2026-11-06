package finalproject.vegastripplanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Parent;

import java.io.IOException;

public class MainPageApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            // 1. Load your Scene Builder FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-page.fxml"));
            Parent root = loader.load();

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            primaryStage.setScene(scene);

            primaryStage.setFullScreenExitHint(""); // Removes the annoying popup text
            primaryStage.setFullScreen(true);

            // It will now render instantly in full-screen
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
