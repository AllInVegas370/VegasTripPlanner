package finalproject.vegastripplanner;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// The one class that talks to the database, so the UI never has to write SQL.
public class PlaceDao {
    private final String dbPath;

    public PlaceDao(String dbPath) {
        this.dbPath = dbPath;
    }

    public List<Place> getAllPlaces() {
        List<Place> places = new ArrayList<>();

        // SQLite silently creates a new EMPTY file if the path is wrong, so check first.
        if (!new File(dbPath).exists()) {
            System.err.println("Database not found at: " + new File(dbPath).getAbsolutePath());
            return places;
        }

        // placesTable only stores ID numbers for category/price/location,
        // so JOIN pulls the real text from the other tables.
        // LEFT JOIN = keep the place even if it has no price or location.
        String sql = """
                SELECT p.place_ID, p.place_name, c.category, pr.price_level,
                       l.street_address, l.latitude, l.longitude
                FROM placesTable p
                JOIN categoryTable c      ON c.category_ID = p.category_ID
                LEFT JOIN priceTable pr   ON pr.price_ID = p.price_ID
                LEFT JOIN locationTable l ON l.location_ID = p.location_ID
                ORDER BY p.place_name
                """;

        try {
            Class.forName("org.sqlite.JDBC");   // makes sure the SQLite driver is loaded
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // try-with-resources closes the connection automatically when finished
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                // getDouble returns 0 for empty cells, so check wasNull() to keep them as null
                Double lat = rs.getDouble("latitude");
                if (rs.wasNull()) lat = null;
                Double lng = rs.getDouble("longitude");
                if (rs.wasNull()) lng = null;

                places.add(new Place(
                        rs.getInt("place_ID"),
                        rs.getString("place_name"),
                        rs.getString("category"),
                        rs.getString("price_level"),
                        rs.getString("street_address"),
                        lat,
                        lng
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return places;
    }
}
