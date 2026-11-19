    package finalproject.vegastripplanner;

    import javafx.animation.PauseTransition;
    import javafx.application.Platform;
    import javafx.event.ActionEvent;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.geometry.Rectangle2D;
    import javafx.scene.Node;
    import javafx.scene.Parent;
    import javafx.scene.Scene;
    import javafx.scene.control.Button;
    import javafx.scene.control.Label;
    import javafx.scene.control.PasswordField;
    import javafx.scene.control.TextField;
    import javafx.stage.Popup;
    import javafx.stage.Screen;
    import javafx.stage.Stage;
    import javafx.util.Duration;

    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;
    import java.util.List;

    public class MainController {
        private static final Path DATA_FILE = Paths.get("UNPW", "users.txt");
        private static final double MESSAGE_SECONDS = 5;

        @FXML
        private Button quitButton, planTrpButton, exploreButton, createAccountButton;

        @FXML
        private TextField usernameLabel;

        @FXML
        private PasswordField passwordLabel;

        @FXML
        private Label welcomeLabel;

        @FXML
        private Popup currentPopup;

        // This buttons quits the entire program
        @FXML
        protected void onQuitButtonClicked() {
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

            try {
                // Load the new browsing page from its FXML file
                FXMLLoader loader = new FXMLLoader(getClass().getResource("browse-places.fxml"));
                Scene scene = new Scene(loader.load(), screenBounds.getWidth(), screenBounds.getHeight());
                window.setScene(scene);
            } catch (IOException e) {
                e.printStackTrace();
                return;   // stay on the current page if the new one fails to load
            }

            window.setFullScreenExitHint("");
            window.setFullScreen(true);
            window.show();
        }

        // This buttons connects to AccountApplication so that a user can make a new account
        @FXML
        protected void onCreateAccountButtonClicked(ActionEvent event) throws IOException {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("account.fxml"));
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

        @FXML
        protected void onSignInButtonClicked(ActionEvent event) throws IOException {
            String username = usernameLabel.getText().trim();
            String password = passwordLabel.getText();

            String firstName = findFirstName(username, password);

            if (firstName == null) {
                showMessage("Incorrect username or password. Please try again.", "#c62828");
                return;
            }

            welcomeLabel.setText("Welcome " + firstName);
            showMessage("Logged in!", "#323232");
        }

        /**
         * Looks through users.txt for a matching username and password.
         * Each line is: username,firstName,lastName,contact,birthday,password
         * Returns the user's first name if they match, or null if not.
         */
        private String findFirstName(String username, String password) {
            if (username.isEmpty() || password.isEmpty()) {
                return null;
            }

            List<String> lines;
            try {
                lines = Files.readAllLines(DATA_FILE);
            } catch (IOException e) {
                e.printStackTrace(); // file missing or unreadable: treat as no match
                return null;
            }

            for (String line : lines) {
                // limit of 6 keeps any commas inside the password (the last field) intact
                String[] parts = line.split(",", 6);
                if (parts.length < 6) {
                    continue; // skip blank or malformed lines
                }
                if (parts[0].equals(username) && parts[5].equals(password)) {
                    return parts[1];
                }
            }
            return null;
        }

        // Small message near the top of the window that disappears on its own.
        // Created in code, so nothing needs to be added to the FXML.
        private void showMessage(String text, String backgroundColor) {
            if (currentPopup != null) {
                currentPopup.hide();
            }

            Label message = new Label(text);
            message.setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: white;"
                    + "-fx-padding: 8 16 8 16; -fx-background-radius: 6;");

            Popup popup = new Popup();
            popup.getContent().add(message);
            currentPopup = popup;

            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            popup.show(stage);

            popup.setX(stage.getX() + (stage.getWidth() - message.getWidth()) / 2);
            popup.setY(stage.getY() + 60);

            PauseTransition delay = new PauseTransition(Duration.seconds(MESSAGE_SECONDS));
            delay.setOnFinished(e -> popup.hide());
            delay.play();
        }
    }