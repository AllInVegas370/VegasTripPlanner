package finalproject.vegastripplanner;

// One place from the database. A record is a class that only holds data:
// Java automatically creates the constructor and getters like name() and category().
public record Place(
        int id,
        String name,
        String category,
        String priceLevel,   // can be null: no places have a price set yet
        String address,      // can be null: many places only have coordinates
        Double latitude,
        Double longitude
) {
    public String categoryDisplay() {
        if (category == null || category.isEmpty()) return "";
        return category.substring(0, 1).toUpperCase() + category.substring(1);
    }

    public String priceDisplay() {
        return (priceLevel == null || priceLevel.isBlank()) ? "Not listed" : priceLevel;
    }

    // Use the street address if there is one, otherwise fall back to the coordinates.
    public String locationDisplay() {
        if (address != null && !address.isBlank()) return address;
        if (latitude != null && longitude != null) return latitude + ", " + longitude;
        return "Not listed";
    }
}
