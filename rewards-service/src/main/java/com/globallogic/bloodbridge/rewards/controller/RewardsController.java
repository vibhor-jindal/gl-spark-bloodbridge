package com.globallogic.bloodbridge.rewards.controller;

import com.globallogic.bloodbridge.rewards.dto.LeaderboardEntry;
import com.globallogic.bloodbridge.rewards.dto.RewardProfileResponse;
import com.globallogic.bloodbridge.rewards.service.RewardsIngestService;
import com.globallogic.bloodbridge.rewards.service.RewardsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardsController {

    private final RewardsQueryService rewardsQueryService;
    private final RewardsIngestService rewardsIngestService;

    @GetMapping("/{donorId}")
    public ResponseEntity<RewardProfileResponse> getProfile(@PathVariable Long donorId) {
        return ResponseEntity.ok(rewardsQueryService.getProfile(donorId));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(@RequestParam String city) {
        return ResponseEntity.ok(rewardsQueryService.getLeaderboard(city));
    }

    /** Called by request-service when a donation is fulfilled (Kafka is backup). */
    @PostMapping("/{donorId}/credit")
    public ResponseEntity<RewardProfileResponse> creditDonation(@PathVariable Long donorId) {
        rewardsIngestService.creditDonation(donorId);
        return ResponseEntity.ok(rewardsQueryService.getProfile(donorId));
    }

    /** Repair totals from known fulfilled donation count (admin/ops). */
    @PutMapping("/{donorId}/sync")
    public ResponseEntity<RewardProfileResponse> syncTotals(
            @PathVariable Long donorId,
            @RequestParam int donationCount) {
        rewardsIngestService.syncTotals(donorId, donationCount);
        return ResponseEntity.ok(rewardsQueryService.getProfile(donorId));
    }
}
