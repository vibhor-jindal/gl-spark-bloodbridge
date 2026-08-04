package com.globallogic.bloodbridge.rewards.repository;

import com.globallogic.bloodbridge.rewards.model.BadgeAward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BadgeAwardRepository extends JpaRepository<BadgeAward, Long> {
    List<BadgeAward> findByDonorId(Long donorId);
    boolean existsByDonorIdAndBadgeName(Long donorId, String badgeName);
}
