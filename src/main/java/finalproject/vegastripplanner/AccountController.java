package finalproject.vegastripplanner;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public class AccountController {
    private static final String ERROR_COLOR = "#c62828";

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
                || contact.isEmpty() || password.isEmpty() || birthday == null) {
            showMessage("Please fill in every field.", ERROR_COLOR);
            return;
        }
        if (!password.equals(confirmPassword)) {
            showMessage("Your passwords don't match.", ERROR_COLOR);
            return;
        }

        // The contact box takes an email OR a phone number: anything with an @ is treated as an email
        String email = contact.contains("@") ? contact : null;
        String phone = contact.contains("@") ? null : contact;

        try {
            // Saves to usersTable in AllInVegas.db (the password is stored scrambled, never as typed)
            UserDAO.createAccount(firstName, lastName, username, password, email, phone, birthday);
        } catch (SQLException e) {
            String error = String.valueOf(e.getMessage());
            if (error.contains("usersTable.username")) {
                showMessage("That username is already taken.", ERROR_COLOR);
            } else if (error.contains("usersTable.email")) {
                showMessage("An account with that email already exists.", ERROR_COLOR);
            } else {
                e.printStackTrace();
                showMessage("Couldn't create the account. Please try again.", ERROR_COLOR);
            }
            return;
        }

        usernameField.clear();
        firstNameField.clear();
        lastNameField.clear();
        contactField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        birthdayPicker.setValue(null);

        showMessage("Account created! You can sign in now.", "#323232");
    }

    // Small message that appears on top of the window and disappears after a few seconds.
    private void showMessage(String text, String backgroundColor) {
        Label message = new Label(text);
        message.setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: white;"
                + "-fx-padding: 8 16 8 16; -fx-background-radius: 6;");

        Popup popup = new Popup();
        popup.getContent().add(message);

        Stage stage = (Stage) usernameField.getScene().getWindow();
        popup.show(stage);

        // Center it horizontally near the top of the window
        popup.setX(stage.getX() + (stage.getWidth() - message.getWidth()) / 2);
        popup.setY(stage.getY() + 60);

        PauseTransition delay = new PauseTransition(Duration.seconds(6));
        delay.setOnFinished(e -> popup.hide());
        delay.play();
    }
}