package com.globallogic.bloodbridge.donor.controller;

import com.globallogic.bloodbridge.donor.dto.DonorRequest;
import com.globallogic.bloodbridge.donor.dto.DonorResponse;
import com.globallogic.bloodbridge.donor.service.DonorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
public class DonorController {

    private final DonorService donorService;

    @PostMapping
    public ResponseEntity<DonorResponse> registerDonor(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody DonorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donorService.registerDonor(userId, request));
    }

    @GetMapping("/{donorId}")
    public ResponseEntity<DonorResponse> getDonor(@PathVariable Long donorId) {
        return ResponseEntity.ok(donorService.getDonor(donorId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DonorResponse>> search(
            @RequestParam String bloodGroup,
            @RequestParam String city) {
        return ResponseEntity.ok(donorService.search(bloodGroup, city));
    }

    @PatchMapping("/{donorId}/availability")
    public ResponseEntity<DonorResponse> updateAvailability(
            @PathVariable Long donorId,
            @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(donorService.updateAvailability(donorId, body.get("isAvailable")));
    }

    @PostMapping("/{donorId}/donations")
    public ResponseEntity<DonorResponse> recordDonation(@PathVariable Long donorId) {
        return ResponseEntity.ok(donorService.recordDonation(donorId));
    }
}
