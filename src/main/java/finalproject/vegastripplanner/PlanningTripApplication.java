package finalproject.vegastripplanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class PlanningTripApplication {

   private final Stage primaryStage = new Stage();

    public void showWindow(Stage mainStage) throws IOException {
        Parent root;
        root = FXMLLoader.load(getClass().getResource("main-page.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setScene(scene);

        primaryStage.setFullScreen(true);

        primaryStage.show();
    }
}

