package finalproject.vegastripplanner;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class AccountApplication {
    private final Stage primaryStage = new Stage();

    public void showWindow(Stage mainStage) throws IOException {
        /*Parent root;
        root = FXMLLoader.load(getClass().getResource("account.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setScene(scene);

        primaryStage.setFullScreen(true);

        primaryStage.show();*/

        try {
            // 1. Load your Scene Builder FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("your_layout.fxml"));
            Parent root = loader.load();

            // 2. Fetch the team member's monitor screen bounds
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // 3. Instantiate the scene using their exact screen dimensions right away
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            primaryStage.setScene(scene);

            // 4. PRE-CONFIGURE full-screen states BEFORE showing the window
            primaryStage.setFullScreenExitHint(""); // Removes the annoying popup text
            primaryStage.setFullScreen(true);       // Arms the full-screen flag early

            // 5. Finally, reveal the window
            // It will now render instantly in full-screen without the "shrink and snap"
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
