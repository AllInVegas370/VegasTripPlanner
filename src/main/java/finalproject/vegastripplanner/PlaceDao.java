package finalproject.vegastripplanner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The one class that reads places from the database, so the UI never has to write SQL.
// Connections come from Database.connect(), the same as UserDAO.
public class PlaceDao {

    public List<Place> getAllPlaces() {
        List<Place> places = new ArrayList<>();

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

        // try-with-resources closes the connection automatically when finished
        try (Connection conn = Database.connect();
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

    // Every place keyed by its place_ID, for pages that only know the IDs
    // (favorites, itinerary stops) but want the category, price, address or map position.
    public Map<Integer, Place> getPlacesById() {
        Map<Integer, Place> byId = new LinkedHashMap<>();
        for (Place p : getAllPlaces()) {
            byId.put(p.id(), p);
        }
        return byId;
    }
}