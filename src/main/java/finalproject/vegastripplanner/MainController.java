    package finalproject.vegastripplanner;

    import javafx.application.Platform;
    import javafx.event.ActionEvent;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.geometry.Rectangle2D;
    import javafx.scene.Node;
    import javafx.scene.Parent;
    import javafx.scene.Scene;
    import javafx.scene.control.Button;
    import javafx.stage.Screen;
    import javafx.stage.Stage;

    import java.io.IOException;

    public class MainController {



        @FXML
        private Button quitButton, planTrpButton, exploreButton, createAccountButton;

        // This buttons quits the entire program
        @FXML
        protected void onQuitButtonClicked(){
            Platform.exit();
        }

        // This buttons connects to PlanningTripApplication
        @FXML
        protected void onPlanTripButtonClicked(ActionEvent event) throws IOException {
            try {
                // 1. Load your Scene Builder FXML file
                FXMLLoader loader = new FXMLLoader(getClass().getResource("planning-trip.fxml"));
                Parent root = loader.load();

                // 2. Fetch the team member's monitor screen bounds
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

                Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();

                // 3. Instantiate the scene using their exact screen dimensions right away
                Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
                window.setScene(scene);

                // 4. PRE-CONFIGURE full-screen states BEFORE showing the window
                window.setFullScreenExitHint(""); // Removes the annoying popup text
                window.setFullScreen(true);       // Arms the full-screen flag early

                // 5. Finally, reveal the window
                // It will now render instantly in full-screen without the "shrink and snap"
                window.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // This buttons connects to ExploringApplication
        @FXML
        protected void onExploreButtonClicked(ActionEvent event) {
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Build the Exploring page at full-screen size and show it in this same window
            Scene scene = new ExploringApplication()
                    .createScene(screenBounds.getWidth(), screenBounds.getHeight());
            window.setScene(scene);

            window.setFullScreenExitHint("");
            window.setFullScreen(true);
            window.show();
        }

        // This buttons connects to AccountApplication so that a user can make a new account
        @FXML
        protected void onCreateAccountButtonClicked(ActionEvent event) throws IOException{
            try {
                // 1. Load your Scene Builder FXML file
                FXMLLoader loader = new FXMLLoader(getClass().getResource("account.fxml"));
                Parent root = loader.load();

                // 2. Fetch the team member's monitor screen bounds
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

                Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();

                // 3. Instantiate the scene using their exact screen dimensions right away
                Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
                window.setScene(scene);

                // 4. PRE-CONFIGURE full-screen states BEFORE showing the window
                window.setFullScreenExitHint(""); // Removes the annoying popup text
                window.setFullScreen(true);       // Arms the full-screen flag early

                // 5. Finally, reveal the window
                // It will now render instantly in full-screen without the "shrink and snap"
                window.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
