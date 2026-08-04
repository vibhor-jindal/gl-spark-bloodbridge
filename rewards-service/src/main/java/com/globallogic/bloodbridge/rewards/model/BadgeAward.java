package com.globallogic.bloodbridge.rewards.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "badge_awards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeAward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long donorId;

    @Column(nullable = false)
    private String badgeName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime awardedAt;

    @PrePersist
    protected void onCreate() {
        this.awardedAt = LocalDateTime.now();
    }
}
