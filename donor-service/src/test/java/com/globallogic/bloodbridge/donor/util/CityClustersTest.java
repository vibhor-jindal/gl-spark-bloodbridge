package com.globallogic.bloodbridge.donor.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CityClustersTest {

    @Test
    void delhiNcrIncludesNearbyPeers() {
        assertThat(CityClusters.searchCities("Delhi"))
                .contains("delhi", "noida", "gurgaon", "gurugram", "ghaziabad", "faridabad", "greater noida", "new delhi");
    }

    @Test
    void gurgaonAndGurugramAreSameCity() {
        assertThat(CityClusters.isSameCity("Gurgaon", "Gurugram")).isTrue();
        assertThat(CityClusters.isSameCity("New Delhi", "Delhi")).isTrue();
        assertThat(CityClusters.isSameCity("Delhi", "Mumbai")).isFalse();
    }

    @Test
    void mumbaiHasNoClusterPeers() {
        assertThat(CityClusters.nearbyCities("Mumbai")).isEmpty();
        assertThat(CityClusters.searchCities("Mumbai")).containsExactly("mumbai");
    }
}
