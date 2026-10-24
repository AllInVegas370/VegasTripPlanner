package finalproject.vegastripplanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    String SavedUserName = "";

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));

        Pane base = new Pane();
        base.setStyle("-fx-background-color: white;");

        TextField textbox = new TextField();
        textbox.setPromptText("Enter Name");

        Label text = new Label("Enter Name");

        Button Browse = new Button("Just Browsing");
        Button Plan = new Button("Planning Trip");

        //Setting Browse Button

        Browse.setLayoutX(450);
        Browse.setLayoutY(300);

        Browse.setPrefWidth(100);
        Browse.setPrefHeight(100);

        Browse.setOnAction(actionEvent -> {
            try{
                ExploringApplication explore = new ExploringApplication();
                explore.start(stage);
            } catch(Exception e){
                System.err.println("Could not open the exploring view.");
                e.printStackTrace();
            }
        });


        //Setting Plan Button

        Plan.setLayoutX(200);
        Plan.setLayoutY(300);

        Plan.setPrefWidth(100);
        Plan.setPrefHeight(100);

        Plan.setOnAction(actionEvent -> {
            try{
                PlanningTripApplication plan = new PlanningTripApplication();
                plan.showWindow(stage);
            } catch(Exception e){
                System.err.println("Could not open the trip planner view.");
                e.printStackTrace();
            }
        });

        //Setting text

        text.setLayoutX(350);
        text.setLayoutY(50);

        //Setting textbox
        textbox.setLayoutX(290);
        textbox.setLayoutY(75);

        textbox.setPrefWidth(200);
        textbox.setPrefHeight(50);

        textbox.setOnAction(actionEvent -> {
            SavedUserName = textbox.getText();
            System.out.println("Saved: " + SavedUserName);

            text.setText("Hello " + SavedUserName);

        });

        base.getChildren().add(text);
        base.getChildren().add(textbox);
        base.getChildren().add(Browse);
        base.getChildren().add(Plan);

        Scene scene = new Scene(base, 800, 500);


        stage.setTitle("JavaFX Textbox");
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}