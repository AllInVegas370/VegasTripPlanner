package finalproject.vegastripplanner;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.prefs.Preferences;

// Welcome / Sign in page (main-page.fxml)
public class MainController {

    // "Remember me" keeps the last username on this computer (never the password)
    private static final Preferences SAVED = Preferences.userNodeForPackage(MainController.class);
    private static final String REMEMBERED_USERNAME = "rememberedUsername";

    @FXML private Button quitButton, exploreButton, createAccountButton, signInButton, showPasswordButton;
    @FXML private TextField usernameLabel;
    @FXML private PasswordField passwordLabel;
    @FXML private Label welcomeLabel;
    @FXML private CheckBox rememberMeCheck;

    @FXML
    public void initialize() {
        Ui.attachShowButton(passwordLabel, showPasswordButton);

        String remembered = SAVED.get(REMEMBERED_USERNAME, "");
        if (!remembered.isEmpty()) {
            usernameLabel.setText(remembered);
            rememberMeCheck.setSelected(true);
            Platform.runLater(passwordLabel::requestFocus);
        }
    }

    @FXML
    protected void onQuitButtonClicked() {
        Platform.exit();
    }

    @FXML
    protected void onSignInButtonClicked() {
        String username = usernameLabel.getText().trim();
        String password = passwordLabel.getText();

        if (username.isEmpty() || password.isEmpty()) {
            Ui.toast(signInButton, "Please enter your username (or email) and password.", true);
            return;
        }

        UserDAO.User user;
        try {
            user = UserDAO.login(username, password);   // checks usersTable in AllInVegas.db
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(signInButton, "Couldn't reach the database. Please try again.", true);
            return;
        }

        if (user == null) {
            passwordLabel.clear();
            Ui.toast(signInButton, "Incorrect username or password. Please try again.", true);
            return;
        }

        if (rememberMeCheck.isSelected()) {
            SAVED.put(REMEMBERED_USERNAME, username);
        } else {
            SAVED.remove(REMEMBERED_USERNAME);
        }

        Session.login(user);   // every other page can now see who is signed in
        Navigator.go(signInButton, Navigator.DASHBOARD);
    }

    @FXML
    protected void onForgotPasswordClicked() {
        Ui.info(signInButton, "Password reset isn't available yet.\n"
                + "If you're signed in on another session, you can change it in Account settings.");
    }

    @FXML
    protected void onCreateAccountButtonClicked() {
        Navigator.go(createAccountButton, Navigator.CREATE_ACCOUNT);
    }

    // "Just looking? Browse as a guest"
    @FXML
    protected void onExploreButtonClicked() {
        Navigator.go(exploreButton, Navigator.BROWSE);
    }
}