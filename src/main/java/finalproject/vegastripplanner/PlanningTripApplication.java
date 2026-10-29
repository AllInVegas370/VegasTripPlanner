package finalproject.vegastripplanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class PlanningTripApplication {

   private final Stage primaryStage = new Stage();

    public void showWindow(Stage mainStage) throws IOException {
        // Using getClass() ensures it looks in the correct resource folder relative to this class
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("planning-trip.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 600, 500);
        primaryStage.setScene(scene);

        // Optional: Blocks the HelloApplication window until this popup is closed
        primaryStage.initModality(Modality.APPLICATION_MODAL);

        mainStage.close();

        primaryStage.show();
    }
}

