package com.globallogic.bloodbridge.request.controller;

import com.globallogic.bloodbridge.request.dto.RequestCreateRequest;
import com.globallogic.bloodbridge.request.dto.RequestResponse;
import com.globallogic.bloodbridge.request.dto.StatusUpdateRequest;
import com.globallogic.bloodbridge.request.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @PostMapping
    public ResponseEntity<RequestResponse> createRequest(
            @RequestHeader("X-User-Id") Long requesterId,
            @Valid @RequestBody RequestCreateRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requestService.createRequest(requesterId, dto));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<RequestResponse> getRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(requestService.getRequest(requestId));
    }

    @GetMapping
    public ResponseEntity<List<RequestResponse>> getMyRequests(@RequestHeader("X-User-Id") Long requesterId) {
        return ResponseEntity.ok(requestService.getRequestsByRequester(requesterId));
    }

    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<RequestResponse> cancelRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(requestService.cancelRequest(requestId));
    }

    @PatchMapping("/{requestId}/status")
    public ResponseEntity<RequestResponse> updateStatus(
            @PathVariable Long requestId,
            @Valid @RequestBody StatusUpdateRequest dto) {
        return ResponseEntity.ok(requestService.updateStatus(requestId, dto));
    }

    @PatchMapping("/{requestId}/fulfill")
    public ResponseEntity<RequestResponse> markFulfilled(@PathVariable Long requestId) {
        return ResponseEntity.ok(requestService.markFulfilled(requestId));
    }
}
