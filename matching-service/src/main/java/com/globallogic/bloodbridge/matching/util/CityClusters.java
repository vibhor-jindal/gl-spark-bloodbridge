package com.globallogic.bloodbridge.matching.util;

import java.util.Locale;
import java.util.Map;

/** Mirrors donor-service city aliases so match ranking prefers same-city before nearby peers. */
public final class CityClusters {

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

    public static boolean isSameCity(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na == null || nb == null) {
            return false;
        }
        if (na.equals(nb)) {
            return true;
        }
        return ALIASES.getOrDefault(na, na).equals(ALIASES.getOrDefault(nb, nb));
    }
}
