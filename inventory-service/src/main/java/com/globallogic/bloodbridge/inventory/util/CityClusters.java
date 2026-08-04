package com.globallogic.bloodbridge.inventory.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Delhi NCR nearby cities so banks can reserve stock across the metro cluster. */
public final class CityClusters {

    private static final List<String> DELHI_NCR = List.of(
            "delhi",
            "new delhi",
            "noida",
            "greater noida",
            "gurgaon",
            "gurugram",
            "ghaziabad",
            "faridabad"
    );

    private CityClusters() {
    }

    public static String normalize(String city) {
        if (city == null) {
            return null;
        }
        String trimmed = city.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static List<String> searchCities(String city) {
        String key = normalize(city);
        if (key == null) {
            return List.of();
        }
        Set<String> ordered = new LinkedHashSet<>();
        ordered.add(key);
        if (DELHI_NCR.contains(key)) {
            ordered.addAll(DELHI_NCR);
        }
        return List.copyOf(ordered);
    }
}
