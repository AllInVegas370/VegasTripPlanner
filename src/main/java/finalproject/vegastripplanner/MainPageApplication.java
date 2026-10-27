package finalproject.vegastripplanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class MainPageApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-page.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 600, 500);
        primaryStage.setTitle("Start Planning Your Trip!");
        primaryStage.setScene(scene);


        primaryStage.show();
    }
}
