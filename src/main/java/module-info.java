module finalproject.vegastripplanner {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens finalproject.vegastripplanner to com.fasterxml.jackson.databind, javafx.fxml;

    exports finalproject.vegastripplanner;
}