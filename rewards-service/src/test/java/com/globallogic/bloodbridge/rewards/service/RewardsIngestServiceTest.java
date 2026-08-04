package com.globallogic.bloodbridge.rewards.service;

import com.globallogic.bloodbridge.rewards.client.DonorServiceClient;
import com.globallogic.bloodbridge.rewards.dto.DonorDto;
import com.globallogic.bloodbridge.rewards.event.RequestStatusChangedEvent;
import com.globallogic.bloodbridge.rewards.model.RewardAccount;
import com.globallogic.bloodbridge.rewards.repository.BadgeAwardRepository;
import com.globallogic.bloodbridge.rewards.repository.RewardAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardsIngestServiceTest {

    @Mock
    private RewardAccountRepository rewardAccountRepository;

    @Mock
    private BadgeAwardRepository badgeAwardRepository;

    @Mock
    private DonorServiceClient donorServiceClient;

    @InjectMocks
    private RewardsIngestService ingestService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ingestService, "pointsPerDonation", 100);
    }

    @Test
    @DisplayName("US-010 AC1: A FULFILLED status event credits points to the donor's account")
    void testOnStatusChanged_Fulfilled_CreditsPoints() {
        when(rewardAccountRepository.findById(10L)).thenReturn(Optional.empty());
        when(donorServiceClient.getDonor(10L)).thenReturn(new DonorDto(10L, "Amit", "Delhi"));
        when(rewardAccountRepository.save(any(RewardAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        ingestService.onStatusChanged(new RequestStatusChangedEvent(1L, "FULFILLED", 10L, LocalDateTime.now()));

        verify(rewardAccountRepository).save(argThat(a -> a.getTotalPoints() == 100 && a.getDonationCount() == 1));
    }

    @Test
    @DisplayName("A non-FULFILLED status event is ignored")
    void testOnStatusChanged_NonFulfilled_Ignored() {
        ingestService.onStatusChanged(new RequestStatusChangedEvent(1L, "MATCHED", 10L, LocalDateTime.now()));

        verify(rewardAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("A FULFILLED event with no confirmed donor is ignored")
    void testOnStatusChanged_NoDonorId_Ignored() {
        ingestService.onStatusChanged(new RequestStatusChangedEvent(1L, "FULFILLED", null, LocalDateTime.now()));

        verify(rewardAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-010 AC2: Reaching the 5th donation awards the Bronze Lifesaver badge exactly once")
    void testCreditDonation_FifthDonation_AwardsBronzeBadge() {
        RewardAccount account = RewardAccount.builder().donorId(10L).city("Delhi").totalPoints(400).donationCount(4).build();
        when(rewardAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(rewardAccountRepository.save(any(RewardAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(badgeAwardRepository.existsByDonorIdAndBadgeName(10L, "Bronze Lifesaver")).thenReturn(false);

        ingestService.creditDonation(10L);

        assertThat(account.getDonationCount()).isEqualTo(5);
        verify(badgeAwardRepository, times(1)).save(argThat(b -> b.getBadgeName().equals("Bronze Lifesaver")));
    }

    @Test
    @DisplayName("A badge already awarded is not awarded again")
    void testCreditDonation_BadgeAlreadyAwarded_NotDuplicated() {
        RewardAccount account = RewardAccount.builder().donorId(10L).city("Delhi").totalPoints(400).donationCount(4).build();
        when(rewardAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(rewardAccountRepository.save(any(RewardAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(badgeAwardRepository.existsByDonorIdAndBadgeName(10L, "Bronze Lifesaver")).thenReturn(true);

        ingestService.creditDonation(10L);

        verify(badgeAwardRepository, never()).save(any());
    }

    @Test
    @DisplayName("A non-milestone donation count does not award any badge")
    void testCreditDonation_NonMilestone_NoBadge() {
        RewardAccount account = RewardAccount.builder().donorId(10L).city("Delhi").totalPoints(200).donationCount(2).build();
        when(rewardAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(rewardAccountRepository.save(any(RewardAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        ingestService.creditDonation(10L);

        verify(badgeAwardRepository, never()).save(any());
        verify(badgeAwardRepository, never()).existsByDonorIdAndBadgeName(any(), any());
    }
}
