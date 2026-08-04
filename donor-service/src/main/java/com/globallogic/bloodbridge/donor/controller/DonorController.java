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

    @GetMapping("/me")
    public ResponseEntity<DonorResponse> getMyProfile(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(donorService.getByUserId(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DonorResponse>> searchByQuery(
            @RequestParam String bloodGroup,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(donorService.search(bloodGroup, city));
    }

    /** Path form avoids "+" → space corruption from query-string form encoding (used by matching Feign). */
    @GetMapping("/search/{bloodGroup}")
    public ResponseEntity<List<DonorResponse>> searchByPath(
            @PathVariable String bloodGroup,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(donorService.search(bloodGroup, city));
    }

    @GetMapping("/{donorId}")
    public ResponseEntity<DonorResponse> getDonor(@PathVariable Long donorId) {
        return ResponseEntity.ok(donorService.getDonor(donorId));
    }

    @PatchMapping("/{donorId}/availability")
    public ResponseEntity<DonorResponse> updateAvailability(
            @PathVariable Long donorId,
            @RequestBody Map<String, Boolean> body) {
        Boolean available = body == null ? null : body.get("isAvailable");
        if (available == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(donorService.updateAvailability(donorId, available));
    }

    /** PUT alias — OpenFeign's default client cannot send PATCH. */
    @PutMapping("/{donorId}/availability")
    public ResponseEntity<DonorResponse> updateAvailabilityPut(
            @PathVariable Long donorId,
            @RequestBody Map<String, Boolean> body) {
        Boolean available = body == null ? null : body.get("isAvailable");
        if (available == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(donorService.updateAvailability(donorId, available));
    }

    @PostMapping("/{donorId}/donations")
    public ResponseEntity<DonorResponse> recordDonation(@PathVariable Long donorId) {
        return ResponseEntity.ok(donorService.recordDonation(donorId));
    }
}
