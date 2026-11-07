package finalproject.vegastripplanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;


public class PlanningTripController {

    @FXML
    private Button quitButton, returnButton;

    @FXML
    private ChoiceBox<String> budget, children, adults, interests;

    private Trip userTrip;

    @FXML
    private DatePicker startDate, endDate;

    @FXML
    protected void onQuitButtonClicked(){
        Platform.exit();
    }

    @FXML
    protected void onReturnButtonClicked(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        Parent mainPage = loader.load(MainPageApplication.class.getResource("main-page.fxml"));
        Scene mainScene = new Scene(mainPage);

        Stage window = (Stage) ((Node)event.getSource()).getScene().getWindow();

        window.setScene(mainScene);
        window.setMaximized(true);

        window.show();
    }


    @FXML
    public void initialize(){
        budget.getItems().addAll("0","$1000 - $2000", "$2000 - $3000", "$3000 - $4000", "$4000 - $5000", "$5000 - $6000");
        children.getItems().addAll("1", "2", "3", "4", "5", "6+");
        adults.getItems().addAll("1", "2", "3", "4", "5", "6+");
        interests.getItems().addAll("Hotels", "Restaurants", "Bars", "Casinos", "Shows");
    }

    @FXML
    protected void onSubmitButtonClicked(ActionEvent event) throws IOException
    {
        String userBudget = budget.getValue();
        String userChildren = children.getValue();
        String userAdults = adults.getValue();
        String userInterests = interests.getValue();
        LocalDate userStartDate = startDate.getValue();
        LocalDate userEndDate = endDate.getValue();

        if(userBudget == null || userChildren == null ||
                userAdults == null || userInterests == null ||
                userStartDate == null || userEndDate == null)
        {
            return;
        }

        if(userEndDate.isBefore(userStartDate))
        {
            return;
        }

        userTrip = new Trip(userBudget, userChildren, userAdults, userInterests, userStartDate, userEndDate);
        objsToFile(userTrip);

        System.out.println("Saved trip, switching screens");

        FXMLLoader loader = new FXMLLoader();
        Parent mainPage = loader.load(MainPageApplication.class.getResource("main-page.fxml"));
        Scene mainScene = new Scene(mainPage);

        Stage window = (Stage) ((Node)event.getSource()).getScene().getWindow();

        window.setScene(mainScene);
        window.setMaximized(true);

        window.show();
    }

    public class Trip
    {
        private String budget;
        private String children;
        private String adults;
        private String interests;
        private LocalDate startDate;
        private LocalDate endDate;

        public Trip(String budget, String children,
                    String adults, String interests, LocalDate startDate, LocalDate endDate)
        {
            this.budget = budget;//Show only ranges of prices to user
            this.children = children;
            this.adults = adults; //Same as children
            this.interests = interests;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public String getBudget(){return budget;}
        public String getChildren(){return children;}
        public String getAdults(){return adults;}
        public String getInterests(){return interests;}
        public LocalDate getStartDate(){return startDate;}
        public LocalDate getEndDate(){return endDate;}

    }

    public void objsToFile(Trip user)
    {
        String filePath = "userData/users.json";
        File file = new File(filePath);
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());//Handles LocalDate[Start and End dates]

            mapper.writeValue(file, user);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
    public int checkChild() {
        if (children.equals("6+")) {

            Stage childCount = new Stage(); //New Window
            childCount.setTitle("Confirming more than 6 children");

            Pane newPane = new Pane();

            Label quest = new Label("How many children are coming?");
            quest.setLayoutX(100);
            quest.setLayoutY(50);

            TextField amount = new TextField();
            amount.setPromptText("Enter number of children");
            amount.setLayoutX(100);
            amount.setLayoutY(100);

            Button confirm = new Button("Submit");
            confirm.setLayoutX(200);
            confirm.setLayoutY(150);

            confirm.setOnAction(event -> {
                String input = amount.getText();

                if(input.matches("6+"))
                {
                    int numChild = Integer.parseInt(input);

                    if(numChild >= 7)
                    {
                        userTrip.
                    }
                }
                this.children = children;
            });

            newPane.getChildren().addAll(quest, amount, confirm);
            Scene newScene = new Scene(newPane, 500, 300);
            childCount.setScene(newScene);
            childCount.show();

        }
    }
     */
}
