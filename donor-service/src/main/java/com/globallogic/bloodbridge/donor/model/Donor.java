package com.globallogic.bloodbridge.donor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "donors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long donorId;

    @Column(nullable = false, unique = true)
    private Long userId;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Blood group is required")
    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Blood group must be one of A+, A-, B+, B-, AB+, AB-, O+, O-")
    @Column(nullable = false, length = 3)
    private String bloodGroup;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit mobile number")
    @Column(nullable = false, unique = true, length = 10)
    private String phone;

    @Column(length = 150)
    private String email;

    @NotBlank(message = "City is required")
    @Column(nullable = false)
    private String city;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private Boolean isAvailable;

    private LocalDate lastDonationDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isAvailable == null) {
            this.isAvailable = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isEligibleToDonate() {
        if (lastDonationDate == null) {
            return true;
        }
        return lastDonationDate.plusDays(90).isBefore(LocalDate.now())
                || lastDonationDate.plusDays(90).isEqual(LocalDate.now());
    }
}
