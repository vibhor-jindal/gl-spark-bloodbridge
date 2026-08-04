package com.globallogic.bloodbridge.matching.controller;

import com.globallogic.bloodbridge.matching.client.DonorServiceClient;
import com.globallogic.bloodbridge.matching.dto.DonorDto;
import com.globallogic.bloodbridge.matching.dto.MatchResponse;
import com.globallogic.bloodbridge.matching.dto.MatchResponseRequest;
import com.globallogic.bloodbridge.matching.exception.DonorProfileNotFoundException;
import com.globallogic.bloodbridge.matching.service.MatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;
    private final DonorServiceClient donorServiceClient;

    @PostMapping("/requests/{requestId}")
    public ResponseEntity<MatchResponse> matchDonors(@PathVariable Long requestId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchingService.matchDonors(requestId));
    }

    @PostMapping("/requests/{requestId}/responses")
    public ResponseEntity<MatchResponse> respond(
            @PathVariable Long requestId,
            @Valid @RequestBody MatchResponseRequest request) {
        return ResponseEntity.ok(matchingService.processResponse(requestId, request));
    }

    @GetMapping("/requests/{requestId}")
    public ResponseEntity<List<MatchResponse>> getMatches(@PathVariable Long requestId) {
        return ResponseEntity.ok(matchingService.getMatchesForRequest(requestId));
    }

    /** Pending + historical matches for the logged-in donor (resolved via X-User-Id → donor profile). */
    @GetMapping("/mine")
    public ResponseEntity<List<MatchResponse>> myMatches(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "false") boolean pendingOnly) {
        try {
            DonorDto donor = donorServiceClient.getMyDonorProfile(userId);
            List<MatchResponse> matches = pendingOnly
                    ? matchingService.getPendingMatchesForDonor(donor.getDonorId())
                    : matchingService.getMatchesForDonor(donor.getDonorId());
            return ResponseEntity.ok(matches);
        } catch (feign.FeignException.NotFound ex) {
            throw new DonorProfileNotFoundException(userId);
        } catch (feign.FeignException ex) {
            if (ex.status() == 404) {
                throw new DonorProfileNotFoundException(userId);
            }
            throw ex;
        }
    }
}
