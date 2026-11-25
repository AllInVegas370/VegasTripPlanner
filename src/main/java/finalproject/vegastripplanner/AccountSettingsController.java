package finalproject.vegastripplanner;

import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Account settings (account-settings.fxml)
public class AccountSettingsController {

    private static final List<String> TIME_ZONES = List.of(
            "Pacific Time (US & Canada)", "Mountain Time (US & Canada)", "Central Time (US & Canada)",
            "Eastern Time (US & Canada)", "Alaska", "Hawaii", "Outside the US");
    private static final List<String> UNITS = List.of("Miles", "Kilometers");
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private static final String LINK = "settings-link";
    private static final String LINK_ACTIVE = "settings-link-active";
    private static final String INVALID = "aiv-invalid";

    @FXML private NavBarController navBarController;   // the included nav-bar.fxml
    @FXML private ScrollPane pageScroll;
    @FXML private VBox pageContent;
    @FXML private Button profileLink, securityLink, privacyLink;
    @FXML private Node profileCard, securityCard, privacyCard;

    @FXML private Label signedInAsLabel, profileError, passwordChangedLabel;
    @FXML private TextField firstNameField, lastNameField, emailField, phoneField;
    @FXML private ComboBox<String> timeZoneBox, unitsBox;
    @FXML private Button saveButton, changePasswordButton, twoStepButton, downloadButton, deleteAccountButton,
            preferencesLink;

    // sidebar button -> the card it scrolls to
    private Map<Button, Node> sections;

    @FXML
    public void initialize() {
        navBarController.setActive(NavBarController.ACCOUNT);
        sections = Map.of(profileLink, profileCard, securityLink, securityCard, privacyLink, privacyCard);
        highlight(profileLink);

        timeZoneBox.getItems().setAll(TIME_ZONES);
        unitsBox.getItems().setAll(UNITS);

        for (TextField field : List.of(firstNameField, lastNameField, emailField, phoneField)) {
            field.textProperty().addListener((obs, oldText, newText) -> field.getStyleClass().remove(INVALID));
        }

        if (!Session.isLoggedIn()) {
            for (Node n : List.of(saveButton, changePasswordButton, twoStepButton, downloadButton,
                    deleteAccountButton)) {
                n.setDisable(true);
            }
            signedInAsLabel.setText("Not signed in");
            return;
        }
        loadSettings();
    }

    private int userId() {
        return Session.getUser().id();
    }

    private void loadSettings() {
        UserDAO.AccountSettings settings;
        try {
            settings = UserDAO.getAccountSettings(userId());
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(saveButton, "Couldn't load your account.", true);
            return;
        }
        if (settings == null) return;

        signedInAsLabel.setText("✓  Signed in as @" + settings.username());
        firstNameField.setText(settings.firstName());
        lastNameField.setText(settings.lastName());
        emailField.setText(settings.email() == null ? "" : settings.email());
        phoneField.setText(settings.phone() == null ? "" : settings.phone());
        timeZoneBox.setValue(settings.timeZone() != null ? settings.timeZone() : TIME_ZONES.get(0));
        unitsBox.setValue(settings.distanceUnits() != null ? settings.distanceUnits() : UNITS.get(0));
        passwordChangedLabel.setText(passwordAge(settings));
    }

    // "Last updated 2 months ago" (or when the account was made, if never changed)
    private static String passwordAge(UserDAO.AccountSettings settings) {
        LocalDate date = settings.passwordChangedOn() != null ? settings.passwordChangedOn() : settings.memberSince();
        if (date == null) return "Keep it strong and unique.";
        long days = ChronoUnit.DAYS.between(date, LocalDate.now());
        String when;
        if (days <= 0) when = "today";
        else if (days == 1) when = "yesterday";
        else if (days < 31) when = days + " days ago";
        else if (days < 365) when = Trips.plural((int) (days / 30), "month") + " ago";
        else when = "on " + date.format(LONG_DATE);
        return "Last updated " + when;
    }


    // ---------------------------------------------------------------
    //  Sidebar: scroll to a card
    // ---------------------------------------------------------------

    @FXML
    protected void onSidebarClicked(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        highlight(clicked);
        scrollTo(sections.get(clicked));
    }

    private void highlight(Button active) {
        for (Button b : List.of(profileLink, securityLink, privacyLink)) {
            b.getStyleClass().remove(LINK_ACTIVE);
            if (!b.getStyleClass().contains(LINK)) b.getStyleClass().add(LINK);
        }
        active.getStyleClass().add(LINK_ACTIVE);
    }

    private void scrollTo(Node card) {
        double contentHeight = pageContent.getHeight();
        double viewportHeight = pageScroll.getViewportBounds().getHeight();
        if (contentHeight <= viewportHeight) return;   // everything already fits
        Bounds inContent = pageContent.sceneToLocal(card.localToScene(card.getBoundsInLocal()));
        double target = Math.max(0, inContent.getMinY() - 16);
        pageScroll.setVvalue(Math.min(1, target / (contentHeight - viewportHeight)));
    }


    // ---------------------------------------------------------------
    //  Profile ("Save changes")
    // ---------------------------------------------------------------

    @FXML
    protected void onSaveClicked() {
        hideProfileError();
        String first = firstNameField.getText().trim();
        String last = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (first.isEmpty() || last.isEmpty()) {
            if (first.isEmpty()) markInvalid(firstNameField);
            if (last.isEmpty()) markInvalid(lastNameField);
            showProfileError("Please fill in your first and last name.");
            return;
        }
        if (email.isEmpty() && phone.isEmpty()) {
            markInvalid(emailField);
            markInvalid(phoneField);
            showProfileError("Keep at least an email address or a phone number on your account.");
            return;
        }
        if (!email.isEmpty() && !email.contains("@")) {
            markInvalid(emailField);
            showProfileError("That email address doesn't look right.");
            return;
        }

        try {
            UserDAO.User updated = UserDAO.updateProfile(userId(), first, last, email, phone,
                    timeZoneBox.getValue(), unitsBox.getValue());
            Session.updateUser(updated);   // other pages now show the new name
        } catch (SQLException e) {
            if (String.valueOf(e.getMessage()).contains("usersTable.email")) {
                markInvalid(emailField);
                showProfileError("Another account already uses that email address.");
            } else {
                e.printStackTrace();
                showProfileError("Couldn't save your changes. Please try again.");
            }
            return;
        }
        Ui.toast(saveButton, "Your changes are saved.", false);
    }

    private void markInvalid(TextField field) {
        if (!field.getStyleClass().contains(INVALID)) field.getStyleClass().add(INVALID);
    }

    private void showProfileError(String message) {
        profileError.setText(message);
        profileError.setVisible(true);
        profileError.setManaged(true);
        highlight(profileLink);
        scrollTo(profileCard);
    }

    private void hideProfileError() {
        profileError.setVisible(false);
        profileError.setManaged(false);
    }


    // ---------------------------------------------------------------
    //  Sign-in & security
    // ---------------------------------------------------------------

    @FXML
    protected void onChangePasswordClicked() {
        PasswordField current = new PasswordField();
        current.setPromptText("Current password");
        PasswordField fresh = new PasswordField();
        fresh.setPromptText("New password");
        PasswordField repeat = new PasswordField();
        repeat.setPromptText("Repeat new password");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.addRow(0, new Label("Current password"), current);
        form.addRow(1, new Label("New password"), fresh);
        form.addRow(2, new Label("Confirm new password"), repeat);

        ButtonType change = new ButtonType("Change password", ButtonType.OK.getButtonData());
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change password");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, change);
        Ui.prepare(dialog, changePasswordButton);
        javafx.application.Platform.runLater(current::requestFocus);

        Optional<ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() != change) return;

        if (fresh.getText().isEmpty()) {
            Ui.toast(changePasswordButton, "Your new password can't be empty.", true);
            return;
        }
        if (!fresh.getText().equals(repeat.getText())) {
            Ui.toast(changePasswordButton, "The new passwords don't match.", true);
            return;
        }
        try {
            if (!UserDAO.changePassword(userId(), current.getText(), fresh.getText())) {
                Ui.toast(changePasswordButton, "Your current password isn't right.", true);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(changePasswordButton, "Couldn't change your password. Please try again.", true);
            return;
        }
        passwordChangedLabel.setText("Last updated today");
        Ui.toast(changePasswordButton, "Password changed.", false);
    }

    @FXML
    protected void onTwoStepClicked() {
        Ui.info(twoStepButton, "Two-step verification is coming soon.");
    }


    // ---------------------------------------------------------------
    //  Privacy & data
    // ---------------------------------------------------------------

    @FXML
    protected void onDownloadClicked() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save your All In Vegas data");
        chooser.setInitialFileName("all-in-vegas-" + Session.getUser().username() + ".txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
        File file = chooser.showSaveDialog(downloadButton.getScene().getWindow());
        if (file == null) return;   // cancelled

        try {
            Files.writeString(file.toPath(), UserDAO.exportData(userId()), StandardCharsets.UTF_8);
            Ui.toast(downloadButton, "Saved to " + file.getName(), false);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            Ui.toast(downloadButton, "Couldn't save your data.", true);
        }
    }

    @FXML
    protected void onDeleteAccountClicked() {
        boolean sure = Ui.confirm(deleteAccountButton,
                "Delete your account? Your trips, favorites and preferences will be deleted too. This can't be undone.",
                "Delete account");
        if (!sure) return;

        try {
            UserDAO.deleteAccount(userId());
        } catch (SQLException e) {
            e.printStackTrace();
            Ui.toast(deleteAccountButton, "Couldn't delete your account. Please try again.", true);
            return;
        }
        Session.logout();
        Navigator.go(deleteAccountButton, Navigator.WELCOME);
    }

    @FXML
    protected void onEditPreferencesClicked() {
        Navigator.go(preferencesLink, Navigator.PREFERENCES);
    }
}