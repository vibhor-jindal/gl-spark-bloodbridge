package com.globallogic.bloodbridge.rewards.service;

import com.globallogic.bloodbridge.rewards.client.DonorServiceClient;
import com.globallogic.bloodbridge.rewards.dto.DonorDto;
import com.globallogic.bloodbridge.rewards.event.RequestStatusChangedEvent;
import com.globallogic.bloodbridge.rewards.model.BadgeAward;
import com.globallogic.bloodbridge.rewards.model.RewardAccount;
import com.globallogic.bloodbridge.rewards.repository.BadgeAwardRepository;
import com.globallogic.bloodbridge.rewards.repository.RewardAccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class RewardsIngestService {

    private static final Logger log = LoggerFactory.getLogger(RewardsIngestService.class);

    private static final Map<Integer, String> MILESTONE_BADGES = new TreeMap<>(Map.of(
            5, "Bronze Lifesaver",
            10, "Silver Lifesaver",
            25, "Gold Lifesaver"
    ));

    private final RewardAccountRepository rewardAccountRepository;
    private final BadgeAwardRepository badgeAwardRepository;
    private final DonorServiceClient donorServiceClient;

    @Value("${bloodbridge.rewards.points-per-donation:100}")
    private int pointsPerDonation;

    @KafkaListener(
            topics = "request-status-changed-events",
            groupId = "rewards-service",
            properties = {
                    "spring.json.value.default.type=com.globallogic.bloodbridge.rewards.event.RequestStatusChangedEvent",
                    "spring.json.use.type.headers=false"
            })
    public void onStatusChanged(RequestStatusChangedEvent event) {
        if ("FULFILLED".equals(event.getStatus()) && event.getConfirmedDonorId() != null) {
            log.info("Observed FULFILLED for requestId={} donorId={} (credit handled by request-service)",
                    event.getRequestId(), event.getConfirmedDonorId());
        }
    }

    @Transactional
    public void creditDonation(Long donorId) {
        DonorDto donor = resolveDonor(donorId);
        RewardAccount account = rewardAccountRepository.findById(donorId).orElseGet(() ->
                RewardAccount.builder()
                        .donorId(donorId)
                        .donorName(donor != null ? donor.getName() : null)
                        .city(donor != null ? donor.getCity() : null)
                        .totalPoints(0)
                        .donationCount(0)
                        .build());

        if ((account.getDonorName() == null || account.getDonorName().isBlank()) && donor != null) {
            account.setDonorName(donor.getName());
        }
        if ((account.getCity() == null || account.getCity().isBlank()) && donor != null) {
            account.setCity(donor.getCity());
        }

        account.setTotalPoints(account.getTotalPoints() + pointsPerDonation);
        account.setDonationCount(account.getDonationCount() + 1);
        rewardAccountRepository.save(account);

        log.info("Credited {} points to donor id={} name={} (donation #{})",
                pointsPerDonation, donorId, account.getDonorName(), account.getDonationCount());

        String badgeName = MILESTONE_BADGES.get(account.getDonationCount());
        if (badgeName != null && !badgeAwardRepository.existsByDonorIdAndBadgeName(donorId, badgeName)) {
            badgeAwardRepository.save(BadgeAward.builder().donorId(donorId).badgeName(badgeName).build());
            log.info("Awarded badge \"{}\" to donor id={}", badgeName, donorId);
        }
    }

    /** Sets absolute totals (used to repair mismatched points after missed credits). */
    @Transactional
    public void syncTotals(Long donorId, int donationCount) {
        DonorDto donor = resolveDonor(donorId);
        RewardAccount account = rewardAccountRepository.findById(donorId).orElseGet(() ->
                RewardAccount.builder().donorId(donorId).totalPoints(0).donationCount(0).build());
        if (donor != null) {
            account.setDonorName(donor.getName());
            account.setCity(donor.getCity());
        }
        account.setDonationCount(Math.max(0, donationCount));
        account.setTotalPoints(account.getDonationCount() * pointsPerDonation);
        rewardAccountRepository.save(account);

        for (Map.Entry<Integer, String> milestone : MILESTONE_BADGES.entrySet()) {
            if (account.getDonationCount() >= milestone.getKey()
                    && !badgeAwardRepository.existsByDonorIdAndBadgeName(donorId, milestone.getValue())) {
                badgeAwardRepository.save(BadgeAward.builder()
                        .donorId(donorId)
                        .badgeName(milestone.getValue())
                        .build());
            }
        }
        log.info("Synced rewards for donor id={} donations={} points={}",
                donorId, account.getDonationCount(), account.getTotalPoints());
    }

    private DonorDto resolveDonor(Long donorId) {
        try {
            return donorServiceClient.getDonor(donorId);
        } catch (Exception ex) {
            log.warn("Could not resolve donor id={}: {}", donorId, ex.getMessage());
            return null;
        }
    }
}
