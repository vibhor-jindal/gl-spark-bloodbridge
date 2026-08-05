package com.globallogic.bloodbridge.donor.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Practical nearby-city clusters for BloodBridge demo data (primarily Delhi NCR).
 * Matching prefers the request city, then expands to peer cities in the same cluster.
 */
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

    private static final Map<String, String> ALIASES = Map.of(
            "gurugram", "gurgaon",
            "gurgaon", "gurgaon",
            "new delhi", "delhi",
            "delhi", "delhi"
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

    /**
     * Returns nearby/peer cities for the given city (excluding the city itself).
     * Empty when the city is not part of a known cluster.
     */
    public static List<String> nearbyCities(String city) {
        String key = normalize(city);
        if (key == null) {
            return List.of();
        }
        if (DELHI_NCR.contains(key)) {
            return DELHI_NCR.stream().filter(c -> !c.equals(key)).toList();
        }
        return List.of();
    }

    /**
     * Same city first, then nearby cluster peers. Deduplicated, case-normalized keys
     * suitable for case-insensitive DB lookups (pass original casing variants as needed).
     */
    public static List<String> searchCities(String city) {
        String key = normalize(city);
        if (key == null) {
            return List.of();
        }
        Set<String> ordered = new LinkedHashSet<>();
        ordered.add(key);
        ordered.addAll(nearbyCities(key));
        // Include common display variants so IgnoreCase IN matches stored "Gurgaon"/"Gurugram" etc.
        if (ordered.contains("gurgaon") || ordered.contains("gurugram")) {
            ordered.add("gurgaon");
            ordered.add("gurugram");
        }
        if (ordered.contains("delhi") || ordered.contains("new delhi")) {
            ordered.add("delhi");
            ordered.add("new delhi");
        }
        return List.copyOf(ordered);
    }

    public static boolean isSameCity(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na == null || nb == null) {
            return false;
        }
        if (na.equals(nb)) {
            return true;
        }
        String aa = ALIASES.getOrDefault(na, na);
        String bb = ALIASES.getOrDefault(nb, nb);
        return aa.equals(bb);
    }
}
