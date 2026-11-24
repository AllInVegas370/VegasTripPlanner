package finalproject.vegastripplanner;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.List;

/**
 * The "Your day, mapped" panel on the itinerary page.
 * It is drawn with plain JavaFX shapes (no internet or map service needed):
 * a few real Las Vegas streets, a numbered pin for each stop at its real
 * latitude/longitude, and a dashed line joining the stops in order.
 * It zooms to fit the day's stops; the + / - buttons zoom in and out.
 */
public class ItineraryMap extends Pane {

    /** One pin on the map. */
    public record Stop(int number, String name, double latitude, double longitude) {}

    // The design's map is 660 x 540
    public static final double ASPECT = 540.0 / 660.0;

    // Longitude degrees are shorter than latitude degrees this far north (cos 36.1°)
    private static final double LON_SCALE = 0.808;
    // Smallest area shown (in degrees), so one stop isn't zoomed in to nothing
    private static final double MIN_SPAN = 0.012;
    // Shown when there are no stops: the middle of the Strip
    private static final double STRIP_LAT = 36.115, STRIP_LON = -115.171;

    private static final Color BACKGROUND = Color.web("#1b172a");
    private static final Color BLOCK = Color.web("#221c33");
    private static final Color STREET = Color.web("#2f2740");
    private static final Color MAIN_STREET = Color.web("#3d3152");
    private static final Color ROUTE = Color.web("#edb0ff");

    // A few real streets as {latitude, longitude} points (approximate)
    private static final double[][] LAS_VEGAS_BLVD = {
            {36.070, -115.1730}, {36.1017, -115.1727}, {36.1160, -115.1722}, {36.1260, -115.1702},
            {36.1340, -115.1645}, {36.1446, -115.1562}, {36.1600, -115.1480}, {36.1750, -115.1400}};
    private static final double[][] PARADISE_RD = {{36.070, -115.1530}, {36.1750, -115.1530}};
    private static final double[][] TROPICANA_AVE = {{36.1017, -115.2200}, {36.1017, -115.1100}};
    private static final double[][] FLAMINGO_RD = {{36.1160, -115.2200}, {36.1160, -115.1100}};
    private static final double[][] SPRING_MOUNTAIN_RD = {{36.1260, -115.2200}, {36.1260, -115.1100}};
    private static final double[][] SAHARA_AVE = {{36.1446, -115.2200}, {36.1446, -115.1100}};

    private final Pane drawing = new Pane();
    private final Label disclaimer = new Label();
    private List<Stop> stops = List.of();
    private double zoom = 1;

    public ItineraryMap() {
        getStyleClass().add("itin-map");
        setMinSize(0, 0);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        Button zoomIn = new Button("+");
        Button zoomOut = new Button("−");
        zoomIn.setOnAction(e -> setZoom(zoom * 1.5));
        zoomOut.setOnAction(e -> setZoom(zoom / 1.5));
        VBox controls = new VBox(zoomIn, zoomOut);
        controls.getStyleClass().add("itin-map-controls");
        controls.relocate(18, 18);

        disclaimer.getStyleClass().add("itin-map-disclaimer");
        disclaimer.layoutYProperty().bind(heightProperty().subtract(26));
        disclaimer.setLayoutX(18);

        getChildren().addAll(drawing, controls, disclaimer);
        widthProperty().addListener((obs, o, n) -> redraw());
        heightProperty().addListener((obs, o, n) -> redraw());
    }

    /** Shows these stops (in visiting order) and zooms to fit them. */
    public void show(List<Stop> stops, int stopsWithoutLocation) {
        this.stops = List.copyOf(stops);
        this.zoom = 1;
        disclaimer.setText("Schematic · not for navigation"
                + (stopsWithoutLocation > 0 ? "  ·  " + Trips.plural(stopsWithoutLocation, "stop") + " without a map location" : ""));
        redraw();
    }

    private void setZoom(double newZoom) {
        zoom = Math.max(0.4, Math.min(8, newZoom));
        redraw();
    }


    // ---------------------------------------------------------------
    //  Drawing
    // ---------------------------------------------------------------

    // Map position of the middle of the view and how many pixels one (scaled) degree takes
    private double centerX, centerY, pixelsPerDegree;

    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        drawing.getChildren().clear();
        if (w <= 0 || h <= 0) return;

        fitView(w, h);

        Rectangle background = new Rectangle(w, h, BACKGROUND);
        drawing.getChildren().add(background);
        drawBlocks(w, h);

        drawStreet(TROPICANA_AVE, STREET, 9, "TROPICANA AVE");
        drawStreet(FLAMINGO_RD, STREET, 9, "E FLAMINGO RD");
        drawStreet(SPRING_MOUNTAIN_RD, STREET, 9, "SPRING MTN RD / SANDS AVE");
        drawStreet(SAHARA_AVE, STREET, 9, "SAHARA AVE");
        drawStreet(PARADISE_RD, STREET, 9, null);
        drawStreet(LAS_VEGAS_BLVD, MAIN_STREET, 16, null);
        drawStripLabel();

        if (stops.isEmpty()) return;

        // dashed route in visiting order
        Polyline route = new Polyline();
        for (Stop s : stops) {
            route.getPoints().addAll(x(s.longitude()), y(s.latitude()));
        }
        route.setStroke(ROUTE);
        route.setStrokeWidth(3);
        route.getStrokeDashArray().addAll(8.0, 7.0);
        route.setStrokeLineCap(StrokeLineCap.ROUND);
        route.setStrokeLineJoin(StrokeLineJoin.ROUND);
        route.setFill(null);
        drawing.getChildren().add(route);

        for (Stop s : stops) {
            drawPin(s, w);
        }
    }

    // Works out the centre and scale so every stop fits with some room around it
    private void fitView(double w, double h) {
        if (stops.isEmpty()) {
            centerX = STRIP_LON * LON_SCALE;
            centerY = STRIP_LAT;
            pixelsPerDegree = Math.min(w, h) / 0.05 * zoom;
            return;
        }
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Stop s : stops) {
            double sx = s.longitude() * LON_SCALE;
            minX = Math.min(minX, sx);
            maxX = Math.max(maxX, sx);
            minY = Math.min(minY, s.latitude());
            maxY = Math.max(maxY, s.latitude());
        }
        centerX = (minX + maxX) / 2;
        centerY = (minY + maxY) / 2;
        double spanX = Math.max((maxX - minX) * 1.6, MIN_SPAN);
        double spanY = Math.max((maxY - minY) * 1.6, MIN_SPAN);
        pixelsPerDegree = Math.min(w / spanX, h / spanY) * zoom;
    }

    private double x(double longitude) {
        return getWidth() / 2 + (longitude * LON_SCALE - centerX) * pixelsPerDegree;
    }

    private double y(double latitude) {
        return getHeight() / 2 - (latitude - centerY) * pixelsPerDegree;
    }

    // Soft rectangles between the streets, so the map doesn't look empty
    private void drawBlocks(double w, double h) {
        double[] rows = {36.1017, 36.1160, 36.1260, 36.1446};
        double left = x(-115.200), mid = x(-115.1735), right = x(-115.1525), farRight = x(-115.125);
        for (int i = 0; i < rows.length - 1; i++) {
            double top = y(rows[i + 1]) + 10;
            double bottom = y(rows[i]) - 10;
            if (bottom <= top) continue;
            addBlock(left + 10, top, mid - left - 20, bottom - top);
            addBlock(mid + 14, top, right - mid - 26, bottom - top);
            addBlock(right + 10, top, farRight - right - 20, bottom - top);
        }
    }

    private void addBlock(double x, double y, double width, double height) {
        if (width <= 4 || height <= 4) return;
        Rectangle block = new Rectangle(x, y, width, height);
        block.setArcWidth(10);
        block.setArcHeight(10);
        block.setFill(BLOCK);
        drawing.getChildren().add(block);
    }

    private void drawStreet(double[][] points, Color color, double width, String name) {
        Polyline line = new Polyline();
        for (double[] p : points) {
            line.getPoints().addAll(x(p[1]), y(p[0]));
        }
        line.setStroke(color);
        line.setStrokeWidth(width);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        line.setStrokeLineJoin(StrokeLineJoin.ROUND);
        line.setFill(null);
        drawing.getChildren().add(line);

        if (name != null) {
            // label sits on the right-hand side of the street, inside the visible area
            Label label = new Label(name);
            label.getStyleClass().add("itin-map-street");
            double labelX = Math.min(getWidth() - 150, Math.max(70, x(-115.1450)));
            label.relocate(labelX, y(points[0][0]) - 7);
            drawing.getChildren().add(label);
        }
    }

    private void drawStripLabel() {
        Label strip = new Label("LAS VEGAS BLVD  ·  THE STRIP");
        strip.getStyleClass().add("itin-map-strip");
        strip.setRotate(-90);
        // halfway between Tropicana and Flamingo, just left of the boulevard
        double sx = x(-115.1727);
        double sy = y((36.1017 + 36.1160) / 2);
        strip.relocate(sx - 112, sy - 8);
        drawing.getChildren().add(strip);
    }

    private void drawPin(Stop stop, double mapWidth) {
        double px = x(stop.longitude());
        double py = y(stop.latitude());

        Circle glow = new Circle(24, Color.web("#edb0ff", 0.12));
        Circle dot = new Circle(16, ROUTE);
        dot.setStroke(Color.web("#fff3f7"));
        dot.setStrokeWidth(2);
        Label number = new Label(String.valueOf(stop.number()));
        number.getStyleClass().add("itin-map-pin-number");
        StackPane pin = new StackPane(glow, dot, number);
        pin.setAlignment(Pos.CENTER);
        pin.setPrefSize(48, 48);
        pin.relocate(px - 24, py - 24);

        Label name = new Label(stop.name());
        name.getStyleClass().add("itin-map-label");
        name.setMaxWidth(Math.max(120, mapWidth * 0.42));
        drawing.getChildren().addAll(pin, name);

        // put the name on whichever side has more room
        boolean leftSide = px > mapWidth * 0.62;
        name.applyCss();   // so its width is measured with the real font
        double nameWidth = Math.min(name.prefWidth(-1), name.getMaxWidth());
        name.relocate(leftSide ? px - 26 - nameWidth : px + 26, py - 14);
    }
}