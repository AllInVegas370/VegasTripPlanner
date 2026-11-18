package finalproject.vegastripplanner;

import java.util.Comparator;

public record SortOption(String label, Comparator<Place> comparator) { }