package finalproject.vegastripplanner;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;

import java.sql.SQLException;
import java.util.List;

// Create account page (account.fxml)
public class AccountController {

    private static final String INVALID = "aiv-invalid";   // red border, see theme.css

    @FXML private TextField usernameField, firstNameField, lastNameField, contactField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private Button createAccountButton, signInLinkButton, backButton, showPasswordButton, showConfirmButton;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        Ui.attachShowButton(passwordField, showPasswordButton);
        Ui.attachShowButton(confirmPasswordField, showConfirmButton);

        // Typing in a box that was marked wrong clears the red border
        for (TextInputControl field : allFields()) {
            field.textProperty().addListener((obs, oldText, newText) -> field.getStyleClass().remove(INVALID));
        }
    }

    private List<TextInputControl> allFields() {
        return List.of(firstNameField, lastNameField, usernameField, passwordField, confirmPasswordField, contactField);
    }

    @FXML
    protected void onSignInLinkClicked() {
        Navigator.go(signInLinkButton, Navigator.WELCOME);
    }

    @FXML
    protected void onCreateAccountButtonClicked() {
        clearError();
        String username = usernameField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String contact = contactField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Nothing is saved if a field is empty or the passwords don't match
        boolean missing = false;
        for (TextInputControl field : allFields()) {
            if (field.getText() == null || field.getText().isBlank()) {
                markInvalid(field);
                missing = true;
            }
        }
        if (missing) {
            showError("Please fill in every field.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            markInvalid(confirmPasswordField);
            showError("Your passwords don't match.");
            return;
        }

        // The contact box takes an email OR a phone number: anything with an @ is treated as an email
        String email = contact.contains("@") ? contact : null;
        String phone = contact.contains("@") ? null : contact;

        UserDAO.User newUser;
        try {
            // Saves to usersTable in AllInVegas.db (the password is stored scrambled, never as typed).
            // The new design has no birthday field, so it is saved as empty.
            UserDAO.createAccount(firstName, lastName, username, password, email, phone, null);
            newUser = UserDAO.login(username, password);
        } catch (SQLException e) {
            String error = String.valueOf(e.getMessage());
            if (error.contains("usersTable.username")) {
                markInvalid(usernameField);
                showError("That username is already taken.");
            } else if (error.contains("usersTable.email")) {
                markInvalid(contactField);
                showError("An account with that email already exists.");
            } else {
                e.printStackTrace();
                showError("Couldn't create the account. Please try again.");
            }
            return;
        }

        // Sign them straight in and take them to the trip preferences step
        Session.login(newUser);
        PlanningTripController preferences = Navigator.go(createAccountButton, Navigator.PREFERENCES);
        if (preferences != null) {
            preferences.showAccountCreated();
        }
    }

    private void markInvalid(TextInputControl field) {
        if (!field.getStyleClass().contains(INVALID)) field.getStyleClass().add(INVALID);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}