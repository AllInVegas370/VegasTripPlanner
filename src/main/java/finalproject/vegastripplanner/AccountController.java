package finalproject.vegastripplanner;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.StandardOpenOption;

public class AccountController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button signUpButton;

    @FXML
    private Button signInLinkButton;   // skip this line if you already have it

    @FXML
    protected void onSignInLinkClicked() throws IOException {
        FXMLLoader loader = new FXMLLoader(MainPageApplication.class.getResource("main-page.fxml"));
        Parent loginPage = loader.load();
        signInLinkButton.getScene().setRoot(loginPage);
    }

    @FXML
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
    }
}
