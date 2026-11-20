package finalproject.vegastripplanner;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;

public class AccountController {
    private static final Path DATA_FILE = Paths.get("UNPW", "users.txt");

    @FXML
    private TextField usernameField, firstNameField, lastNameField, contactField;

    @FXML
    private DatePicker birthdayPicker;

    @FXML
    private PasswordField passwordField, confirmPasswordField;

    @FXML
    private Button signUpButton, createAccountButton;

    @FXML
    private Button signInLinkButton;   // skip this line if you already have it


    @FXML
    protected void onSignInLinkClicked() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainPageApplication.class.getResource("main-page.fxml"));
        Parent loginPage = loader.load();
        signInLinkButton.getScene().setRoot(loginPage);
    }

    @FXML
    protected void onCreateAccountButtonClicked() throws IOException {
        String username = usernameField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String contact = contactField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        LocalDate birthday = birthdayPicker.getValue(); // null if nothing was picked

        // Nothing is saved if a field is empty or the passwords don't match
        if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty()
                || contact.isEmpty() || password.isEmpty() || birthday == null
                || !password.equals(confirmPassword)) {
            return;
        }

        try {
            // Fields are separated by commas; birthday is saved as yyyy-MM-dd
            String line = String.join(",",
                    username, firstName, lastName, contact, birthday.toString(), password)
                    + System.lineSeparator();
            Files.writeString(DATA_FILE, line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("Saved to: " + DATA_FILE.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        usernameField.clear();
        firstNameField.clear();
        lastNameField.clear();
        contactField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        birthdayPicker.setValue(null);

        showSavedMessage();
    }

    // Small message that appears on top of the window and disappears after 10 seconds.
    private void showSavedMessage() {
        Label message = new Label("Account saved!");
        message.setStyle("-fx-background-color: #323232; -fx-text-fill: white;"
                + "-fx-padding: 8 16 8 16; -fx-background-radius: 6;");

        Popup popup = new Popup();
        popup.getContent().add(message);

        Stage stage = (Stage) usernameField.getScene().getWindow();
        popup.show(stage);

        // Center it horizontally near the top of the window
        popup.setX(stage.getX() + (stage.getWidth() - message.getWidth()) / 2);
        popup.setY(stage.getY() + 60);

        PauseTransition delay = new PauseTransition(Duration.seconds(10));
        delay.setOnFinished(e -> popup.hide());
        delay.play();
    }

    /*@FXML
    protected void onSignUpButtonClicked(ActionEvent event) throws IOException {
        System.out.println("Clicked!!");

        //Save username and password
        String username = usernameField.getText();
        String password = passwordField.getText();

        //Save in UNPW file

        if(!username.isEmpty() && !password.isEmpty()) {
            try {
                Path ListPath = Paths.get("UNPW", "List.txt");
                String line = username + " " + password + "\n";

                // Writes everything in one shot
                Files.writeString(ListPath, line, StandardOpenOption.APPEND);

                System.out.println("Username and Password Saved!");
            } catch (IOException e) {
                System.err.println("Error while saving" + e.getMessage());
            }
        }
    }*/
}