package com.globallogic.bloodbridge.rewards.repository;

import com.globallogic.bloodbridge.rewards.model.RewardAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardAccountRepository extends JpaRepository<RewardAccount, Long> {
    List<RewardAccount> findByCityIgnoreCaseOrderByTotalPointsDesc(String city);
}
