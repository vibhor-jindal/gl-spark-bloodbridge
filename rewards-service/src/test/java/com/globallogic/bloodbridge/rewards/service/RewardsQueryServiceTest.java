package com.globallogic.bloodbridge.rewards.service;

import com.globallogic.bloodbridge.rewards.client.DonorServiceClient;
import com.globallogic.bloodbridge.rewards.dto.LeaderboardEntry;
import com.globallogic.bloodbridge.rewards.dto.RewardProfileResponse;
import com.globallogic.bloodbridge.rewards.model.BadgeAward;
import com.globallogic.bloodbridge.rewards.model.RewardAccount;
import com.globallogic.bloodbridge.rewards.repository.BadgeAwardRepository;
import com.globallogic.bloodbridge.rewards.repository.RewardAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardsQueryServiceTest {

    @Mock
    private RewardAccountRepository rewardAccountRepository;

    @Mock
    private BadgeAwardRepository badgeAwardRepository;

    @Mock
    private DonorServiceClient donorServiceClient;

    @InjectMocks
    private RewardsQueryService queryService;

    @Test
    @DisplayName("US-010 AC3: A donor's profile shows total points, donation count, and badges")
    void testGetProfile_ReturnsPointsAndBadges() {
        RewardAccount account = RewardAccount.builder().donorId(10L).city("Delhi").totalPoints(500).donationCount(5).build();
        when(rewardAccountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(badgeAwardRepository.findByDonorId(10L)).thenReturn(List.of(
                BadgeAward.builder().donorId(10L).badgeName("Bronze Lifesaver").build()));

        RewardProfileResponse profile = queryService.getProfile(10L);

        assertThat(profile.getTotalPoints()).isEqualTo(500);
        assertThat(profile.getDonationCount()).isEqualTo(5);
        assertThat(profile.getBadges()).containsExactly("Bronze Lifesaver");
    }

    @Test
    @DisplayName("A donor with no reward history yet still gets a valid zeroed profile")
    void testGetProfile_NoHistory_ReturnsZeroedProfile() {
        when(rewardAccountRepository.findById(20L)).thenReturn(Optional.empty());
        when(badgeAwardRepository.findByDonorId(20L)).thenReturn(List.of());

        RewardProfileResponse profile = queryService.getProfile(20L);

        assertThat(profile.getTotalPoints()).isZero();
        assertThat(profile.getBadges()).isEmpty();
    }

    @Test
    @DisplayName("US-010 AC4: The leaderboard ranks donors within a city by points, highest first")
    void testGetLeaderboard_RanksByPointsDescending() {
        RewardAccount first = RewardAccount.builder().donorId(1L).city("Delhi").totalPoints(900).donationCount(9).build();
        RewardAccount second = RewardAccount.builder().donorId(2L).city("Delhi").totalPoints(500).donationCount(5).build();
        when(rewardAccountRepository.findByCityIgnoreCaseOrderByTotalPointsDesc("Delhi")).thenReturn(List.of(first, second));

        List<LeaderboardEntry> leaderboard = queryService.getLeaderboard("Delhi");

        assertThat(leaderboard).hasSize(2);
        assertThat(leaderboard.get(0).getDonorId()).isEqualTo(1L);
        assertThat(leaderboard.get(0).getRank()).isEqualTo(1);
        assertThat(leaderboard.get(1).getRank()).isEqualTo(2);
    }
}
