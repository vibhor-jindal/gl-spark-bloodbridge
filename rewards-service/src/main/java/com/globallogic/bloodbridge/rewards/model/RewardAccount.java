package com.globallogic.bloodbridge.rewards.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reward_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardAccount {

    @Id
    private Long donorId;

    private String donorName;

    private String city;

    @Column(nullable = false)
    private Integer totalPoints;

    @Column(nullable = false)
    private Integer donationCount;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.updatedAt = LocalDateTime.now();
        if (this.totalPoints == null) {
            this.totalPoints = 0;
        }
        if (this.donationCount == null) {
            this.donationCount = 0;
        }
    }
}
