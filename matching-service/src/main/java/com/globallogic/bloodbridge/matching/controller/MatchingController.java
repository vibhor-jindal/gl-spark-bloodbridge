package com.globallogic.bloodbridge.matching.controller;

import com.globallogic.bloodbridge.matching.dto.MatchResponse;
import com.globallogic.bloodbridge.matching.dto.MatchResponseRequest;
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
}
