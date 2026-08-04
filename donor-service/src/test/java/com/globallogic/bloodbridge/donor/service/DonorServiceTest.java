package com.globallogic.bloodbridge.donor.service;

import com.globallogic.bloodbridge.donor.dto.DonorRequest;
import com.globallogic.bloodbridge.donor.dto.DonorResponse;
import com.globallogic.bloodbridge.donor.exception.DonorNotFoundException;
import com.globallogic.bloodbridge.donor.exception.DuplicateDonorException;
import com.globallogic.bloodbridge.donor.model.Donor;
import com.globallogic.bloodbridge.donor.repository.DonorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonorServiceTest {

    @Mock
    private DonorRepository donorRepository;

    @InjectMocks
    private DonorService donorService;

    private DonorRequest validRequest;
    private Donor savedDonor;

    @BeforeEach
    void setUp() {
        validRequest = new DonorRequest("Rahul Sharma", "O+", "9876543210", "rahul@example.com", "Delhi", 28.6139, 77.2090);

        savedDonor = Donor.builder()
                .donorId(1L)
                .userId(100L)
                .name("Rahul Sharma")
                .bloodGroup("O+")
                .phone("9876543210")
                .email("rahul@example.com")
                .city("Delhi")
                .latitude(28.6139)
                .longitude(77.2090)
                .isAvailable(true)
                .build();
    }

    @Test
    @DisplayName("US-001 AC1: Valid donor details create a profile and return a confirmation")
    void testRegisterDonor_Success() {
        when(donorRepository.existsByPhone("9876543210")).thenReturn(false);
        when(donorRepository.save(any(Donor.class))).thenReturn(savedDonor);

        DonorResponse response = donorService.registerDonor(100L, validRequest);

        assertThat(response.getDonorId()).isEqualTo(1L);
        assertThat(response.getBloodGroup()).isEqualTo("O+");
        verify(donorRepository, times(1)).save(any(Donor.class));
    }

    @Test
    @DisplayName("US-001 AC2: Registering with a phone number already on file is rejected")
    void testRegisterDonor_DuplicatePhone() {
        when(donorRepository.existsByPhone("9876543210")).thenReturn(true);

        assertThatThrownBy(() -> donorService.registerDonor(100L, validRequest))
                .isInstanceOf(DuplicateDonorException.class);

        verify(donorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fetching an unknown donor id throws DonorNotFoundException")
    void testGetDonor_NotFound() {
        when(donorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> donorService.getDonor(99L))
                .isInstanceOf(DonorNotFoundException.class);
    }

    @Test
    @DisplayName("US-003: Search returns only available donors matching blood group and city")
    void testSearch_ReturnsEligibleAvailableDonors() {
        when(donorRepository.findByBloodGroupAndCityIgnoreCaseAndIsAvailableTrue("O+", "Delhi"))
                .thenReturn(List.of(savedDonor, savedDonor, savedDonor));

        List<DonorResponse> results = donorService.search("O+", "Delhi");

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getBloodGroup()).isEqualTo("O+");
        verify(donorRepository, never()).findAvailableByBloodGroupAndCities(any(), any());
    }

    @Test
    @DisplayName("Nearby cities: when same-city donors are few, include Delhi NCR peers")
    void testSearch_ExpandsToNearbyCitiesWhenLocalFew() {
        Donor noidaDonor = Donor.builder()
                .donorId(2L)
                .userId(101L)
                .name("Noida Donor")
                .bloodGroup("O+")
                .city("Noida")
                .isAvailable(true)
                .build();

        when(donorRepository.findByBloodGroupAndCityIgnoreCaseAndIsAvailableTrue("O+", "Delhi"))
                .thenReturn(List.of(savedDonor));
        when(donorRepository.findAvailableByBloodGroupAndCities(eq("O+"), any()))
                .thenReturn(List.of(savedDonor, noidaDonor));

        List<DonorResponse> results = donorService.search("O+", "Delhi");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCity()).isEqualTo("Delhi");
        assertThat(results.get(1).getCity()).isEqualTo("Noida");
    }

    @Test
    @DisplayName("Nearby cities: New Delhi request matches Delhi/Noida cluster donors")
    void testSearch_NewDelhiUsesNcrCluster() {
        when(donorRepository.findByBloodGroupAndCityIgnoreCaseAndIsAvailableTrue("O+", "New Delhi"))
                .thenReturn(List.of());
        when(donorRepository.findAvailableByBloodGroupAndCities(eq("O+"), any()))
                .thenReturn(List.of(savedDonor));

        List<DonorResponse> results = donorService.search("O+", "New Delhi");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCity()).isEqualTo("Delhi");
    }

    @Test
    @DisplayName("US-001 AC4: A donor who donated within 90 days is not eligible again yet")
    void testIsEligibleToDonate_WithinCooldown_ReturnsFalse() {
        savedDonor.setLastDonationDate(LocalDate.now().minusDays(10));
        assertThat(savedDonor.isEligibleToDonate()).isFalse();
    }

    @Test
    @DisplayName("US-001 AC4: A donor past the 90-day cooldown is eligible again")
    void testIsEligibleToDonate_AfterCooldown_ReturnsTrue() {
        savedDonor.setLastDonationDate(LocalDate.now().minusDays(91));
        assertThat(savedDonor.isEligibleToDonate()).isTrue();
    }

    @Test
    @DisplayName("Recording a donation sets last donation date and marks the donor unavailable")
    void testRecordDonation_SetsCooldownAndUnavailable() {
        when(donorRepository.findById(1L)).thenReturn(Optional.of(savedDonor));
        when(donorRepository.save(any(Donor.class))).thenReturn(savedDonor);

        donorService.recordDonation(1L);

        assertThat(savedDonor.getLastDonationDate()).isEqualTo(LocalDate.now());
        assertThat(savedDonor.getIsAvailable()).isFalse();
    }
}
