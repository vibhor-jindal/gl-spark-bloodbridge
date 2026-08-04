package com.globallogic.bloodbridge.rewards.service;

import com.globallogic.bloodbridge.rewards.client.DonorServiceClient;
import com.globallogic.bloodbridge.rewards.dto.DonorDto;
import com.globallogic.bloodbridge.rewards.dto.LeaderboardEntry;
import com.globallogic.bloodbridge.rewards.dto.RewardProfileResponse;
import com.globallogic.bloodbridge.rewards.model.BadgeAward;
import com.globallogic.bloodbridge.rewards.model.RewardAccount;
import com.globallogic.bloodbridge.rewards.repository.BadgeAwardRepository;
import com.globallogic.bloodbridge.rewards.repository.RewardAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardsQueryService {

    private final RewardAccountRepository rewardAccountRepository;
    private final BadgeAwardRepository badgeAwardRepository;
    private final DonorServiceClient donorServiceClient;

    public RewardProfileResponse getProfile(Long donorId) {
        RewardAccount account = rewardAccountRepository.findById(donorId)
                .orElse(RewardAccount.builder().donorId(donorId).totalPoints(0).donationCount(0).build());

        List<String> badges = badgeAwardRepository.findByDonorId(donorId)
                .stream()
                .map(BadgeAward::getBadgeName)
                .toList();

        String city = account.getCity();
        String donorName = account.getDonorName();
        if (city == null || city.isBlank() || donorName == null || donorName.isBlank()) {
            try {
                DonorDto donor = donorServiceClient.getDonor(donorId);
                if (city == null || city.isBlank()) {
                    city = donor.getCity();
                }
                if (donorName == null || donorName.isBlank()) {
                    donorName = donor.getName();
                }
            } catch (Exception ignored) {
                // profile still returns points even if donor lookup fails
            }
        }

        return RewardProfileResponse.builder()
                .donorId(donorId)
                .donorName(donorName)
                .city(city)
                .totalPoints(account.getTotalPoints())
                .donationCount(account.getDonationCount())
                .badges(badges)
                .build();
    }

    public List<LeaderboardEntry> getLeaderboard(String city) {
        List<RewardAccount> ranked = rewardAccountRepository.findByCityIgnoreCaseOrderByTotalPointsDesc(city);

        return java.util.stream.IntStream.range(0, ranked.size())
                .mapToObj(i -> {
                    RewardAccount account = ranked.get(i);
                    String name = account.getDonorName();
                    if (name == null || name.isBlank()) {
                        try {
                            name = donorServiceClient.getDonor(account.getDonorId()).getName();
                        } catch (Exception ignored) {
                            name = null;
                        }
                    }
                    return LeaderboardEntry.builder()
                            .donorId(account.getDonorId())
                            .donorName(name)
                            .city(account.getCity())
                            .totalPoints(account.getTotalPoints())
                            .donationCount(account.getDonationCount())
                            .rank(i + 1)
                            .build();
                })
                .toList();
    }
}
