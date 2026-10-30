module finalproject.vegastripplanner {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens finalproject.vegastripplanner to javafx.fxml;
    exports finalproject.vegastripplanner;
}