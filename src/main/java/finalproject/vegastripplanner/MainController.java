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
import java.sql.SQLException;

public class MainController {
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
        
    // If someone is already signed in (e.g. they came back with "Return"), greet them.
    @FXML
    public void initialize() {
        if (Session.isLoggedIn()) {
            welcomeLabel.setText("Welcome " + Session.getUser().firstName());
        }
    }

    @FXML
    protected void onSignInButtonClicked(ActionEvent event) throws IOException {
        String username = usernameLabel.getText().trim();
        String password = passwordLabel.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Please enter your username and password.", "#c62828");
            return;
        }

        UserDAO.User user;
        try {
            user = UserDAO.login(username, password);   // checks usersTable in AllInVegas.db
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Couldn't reach the database. Please try again.", "#c62828");
            return;
        }

        if (user == null) {
            showMessage("Incorrect username or password. Please try again.", "#c62828");
            return;
        }

        Session.login(user);   // every other page can now see who is signed in
        passwordLabel.clear();
        welcomeLabel.setText("Welcome " + user.firstName());
        showMessage("Logged in!", "#323232");
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