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
    protected void onSignUpButtonClicked(ActionEvent event) throws IOException {
        System.out.println("Clicked!!");



        //Save username and password
        String username = usernameField.getText();
        String password = passwordField.getText();

        //Save in UNPW file

        if(!username.isEmpty() && !password.isEmpty()){
            try {
                Path ListPath = Paths.get("UNPW","List.txt");
                String line =  username + " " + password + "\n";

                // Writes everything in one shot
                Files.writeString(ListPath, line,StandardOpenOption.APPEND);

                System.out.println("Username and Password Saved!");
            } catch (IOException e) {
                System.err.println("Error while saving" + e.getMessage());
            }

        }


        // 1. Load your Scene Builder FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main-page.fxml"));
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





        ;
    }
}
