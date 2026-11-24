package finalproject.vegastripplanner;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Small helpers every page shares: themed pop-up messages, place photos,
 * rounded corners, and the "Show" button on password boxes.
 */
public final class Ui {

    private static final String THEME = Ui.class.getResource("theme.css").toExternalForm();

    // A photo for one place goes in Images/places/ and is named after the place's ID,
    // for example Images/places/164.jpg. Places without a photo use the fallback.
    private static final String PLACE_IMAGE_FOLDER = "Images/places/";
    private static final String FALLBACK_IMAGE = "Images/vegas-landscape.jpg";
    private static final Map<String, Image> imageCache = new HashMap<>();

    private static Popup currentToast;

    private Ui() {}


    // ---------------------------------------------------------------
    //  Messages
    // ---------------------------------------------------------------

    /** A message with an OK button, styled like the rest of the app. */
    public static void info(Node owner, String message) {
        Alert alert = new Alert(Alert.AlertType.NONE, message, ButtonType.OK);
        prepare(alert, owner);
        alert.showAndWait();
    }

    /** Asks a yes/no question. Returns true if the person clicked the confirm button. */
    public static boolean confirm(Node owner, String message, String confirmText) {
        ButtonType yes = new ButtonType(confirmText, ButtonType.OK.getButtonData());
        Alert alert = new Alert(Alert.AlertType.NONE, message, ButtonType.CANCEL, yes);
        prepare(alert, owner);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == yes;
    }

    /** Asks for one line of text. Empty Optional = cancelled. */
    public static Optional<String> askText(Node owner, String title, String label, String startingText) {
        TextInputDialog dialog = new TextInputDialog(startingText);
        dialog.setTitle(title);
        dialog.setContentText(label);
        prepare(dialog, owner);
        return dialog.showAndWait().map(String::trim);
    }

    /** Gives any Dialog the app's dark theme and puts it over the current window. */
    public static void prepare(Dialog<?> dialog, Node owner) {
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        if (dialog.getTitle() == null || dialog.getTitle().isBlank()) dialog.setTitle("All In Vegas");
        dialog.getDialogPane().getStylesheets().add(THEME);
        if (owner != null && owner.getScene() != null) {
            Window window = owner.getScene().getWindow();
            if (window != null) dialog.initOwner(window);
        }
    }

    /** A short message near the top of the window that disappears by itself. */
    public static void toast(Node owner, String message, boolean isError) {
        if (owner == null || owner.getScene() == null) return;
        Window window = owner.getScene().getWindow();
        if (currentToast != null) currentToast.hide();

        Label label = new Label(message);
        label.getStylesheets().add(THEME);
        label.getStyleClass().add("aiv-toast");
        if (isError) label.getStyleClass().add("aiv-toast-error");

        Popup popup = new Popup();
        popup.getContent().add(label);
        currentToast = popup;
        popup.show(window);
        popup.setX(window.getX() + (window.getWidth() - label.getWidth()) / 2);
        popup.setY(window.getY() + 64);

        PauseTransition delay = new PauseTransition(Duration.seconds(isError ? 6 : 4));
        delay.setOnFinished(e -> popup.hide());
        delay.play();
    }


    // ---------------------------------------------------------------
    //  Password "Show" button
    // ---------------------------------------------------------------

    /**
     * Makes a "Show"/"Hide" button reveal what is typed in a PasswordField.
     * In the FXML, put the PasswordField and the Button together in a StackPane
     * (button aligned CENTER_RIGHT). A plain TextField that shares the same text
     * is slipped in behind the button and shown instead while "Show" is on.
     */
    public static void attachShowButton(PasswordField passwordField, Button showButton) {
        StackPane holder = (StackPane) passwordField.getParent();

        TextField plain = new TextField();
        plain.setPromptText(passwordField.getPromptText());
        plain.textProperty().bindBidirectional(passwordField.textProperty());
        plain.setOnAction(passwordField.getOnAction());
        plain.setVisible(false);
        plain.setManaged(false);
        holder.getChildren().add(holder.getChildren().indexOf(passwordField) + 1, plain);

        StackPane.setAlignment(showButton, Pos.CENTER_RIGHT);
        showButton.setFocusTraversable(false);
        showButton.setOnAction(e -> {
            boolean show = !plain.isVisible();
            plain.setVisible(show);
            plain.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);
            showButton.setText(show ? "Hide" : "Show");
            (show ? plain : passwordField).requestFocus();
            (show ? plain : passwordField).end();
        });
    }


    // ---------------------------------------------------------------
    //  Photos
    // ---------------------------------------------------------------

    /** The photo for a place (Images/places/<id>.jpg) or the Vegas fallback, loaded at the given width. */
    public static Image placeImage(int placeId, int width) {
        URL found = Ui.class.getResource(PLACE_IMAGE_FOLDER + placeId + ".jpg");
        if (found == null) found = Ui.class.getResource(FALLBACK_IMAGE);
        if (found == null) return null;

        String path = found.toExternalForm();
        // Loads in the background so pages open quickly; the photo pops in when ready
        return imageCache.computeIfAbsent(path + "@" + width, key -> new Image(path, width, 0, true, true, true));
    }

    /** An ImageView of exactly w x h that fills the box with the photo (cropping, never stretching). */
    public static ImageView coverImage(Image image, double w, double h) {
        ImageView view = new ImageView();
        showCover(view, image, w, h);
        return view;
    }

    /** Fills a w x h box with the image, cropping the edges instead of stretching it. */
    public static void showCover(ImageView view, Image image, double w, double h) {
        view.setImage(image);
        view.setPreserveRatio(false);
        view.setFitWidth(w);
        view.setFitHeight(h);
        if (image == null) {
            view.setViewport(null);
            return;
        }
        if (image.getProgress() < 1) {
            // still loading in the background: crop once it has arrived
            image.progressProperty().addListener((obs, old, progress) -> {
                if (progress.doubleValue() >= 1 && view.getImage() == image) cropToCover(view, image, w, h);
            });
        }
        cropToCover(view, image, w, h);
    }

    /**
     * A box of fixed height whose photo always fills its (changing) width,
     * for cards that stretch with the window.
     */
    public static StackPane coverBox(Image image, double height) {
        ImageView view = new ImageView();
        view.setManaged(false);   // the photo follows the box's size, never the other way round
        StackPane box = new StackPane(view);
        box.setMinHeight(height);
        box.setPrefHeight(height);
        box.setMaxHeight(height);
        box.setMinWidth(0);
        box.widthProperty().addListener((obs, oldW, w) -> {
            if (w.doubleValue() > 0) showCover(view, image, w.doubleValue(), height);
        });
        return box;
    }

    private static void cropToCover(ImageView view, Image image, double w, double h) {
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
            view.setViewport(null);
            return;
        }
        double scale = Math.max(w / image.getWidth(), h / image.getHeight());
        double viewW = w / scale;
        double viewH = h / scale;
        view.setViewport(new Rectangle2D((image.getWidth() - viewW) / 2, (image.getHeight() - viewH) / 2, viewW, viewH));
    }

    /** Rounds a node's corners (like CSS border-radius + overflow: hidden). */
    public static void roundCorners(Node node, double width, double height, double radius) {
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        node.setClip(clip);
    }

    /** Same as above, but the corners follow the region as it is resized. */
    public static void roundCorners(Region region, double radius) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }

    /** "restaurant" -> "Restaurants" (the names used on the filter chips). */
    public static String categoryLabel(String category) {
        if (category == null) return "";
        return switch (category) {
            case "casino" -> "Casinos";
            case "hotel" -> "Hotels";
            case "entertainment" -> "Entertainment";
            case "attraction" -> "Attractions";
            case "restaurant" -> "Restaurants";
            case "bar" -> "Bars";
            case "shopping" -> "Shopping";
            default -> category.isEmpty() ? "" : category.substring(0, 1).toUpperCase() + category.substring(1);
        };
    }

    /** Just the street part of an address, e.g. "3600 South Las Vegas Boulevard". */
    public static String shortAddress(String address) {
        if (address == null || address.isBlank()) return "";
        int comma = address.indexOf(',');
        return comma > 0 ? address.substring(0, comma) : address;
    }
}