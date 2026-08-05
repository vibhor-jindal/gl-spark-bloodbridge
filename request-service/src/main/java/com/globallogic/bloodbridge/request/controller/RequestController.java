package com.globallogic.bloodbridge.request.controller;

import com.globallogic.bloodbridge.request.dto.*;
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

    @GetMapping("/open")
    public ResponseEntity<List<RequestResponse>> listOpen(@RequestParam(required = false) String city) {
        return ResponseEntity.ok(requestService.listOpen(city));
    }

    /** Blood bank's reserved / out-for-delivery / fulfilled requests (mirrors donor match alerts). */
    @GetMapping("/bank")
    public ResponseEntity<List<RequestResponse>> listForBloodBank(
            @RequestHeader("X-User-Id") Long bloodBankUserId) {
        return ResponseEntity.ok(requestService.listForBloodBank(bloodBankUserId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<RequestResponse>> listAll() {
        return ResponseEntity.ok(requestService.listAll());
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

    /** PUT alias — OpenFeign's default client cannot send PATCH. */
    @PutMapping("/{requestId}/status")
    public ResponseEntity<RequestResponse> updateStatusPut(
            @PathVariable Long requestId,
            @Valid @RequestBody StatusUpdateRequest dto) {
        return ResponseEntity.ok(requestService.updateStatus(requestId, dto));
    }

    @PostMapping("/{requestId}/reserve-bank")
    public ResponseEntity<RequestResponse> reserveFromBloodBank(
            @PathVariable Long requestId,
            @Valid @RequestBody BankReserveRequest dto) {
        return ResponseEntity.ok(requestService.reserveFromBloodBank(requestId, dto));
    }

    @PostMapping("/{requestId}/start-delivery")
    public ResponseEntity<RequestResponse> startDelivery(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok(requestService.startDelivery(requestId, userId, role));
    }

    /** Regenerate OTP after expiry while still OUT_FOR_DELIVERY (bank owner, confirmed donor, or admin). */
    @PostMapping("/{requestId}/restart-delivery")
    public ResponseEntity<RequestResponse> restartDelivery(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok(requestService.restartDelivery(requestId, userId, role));
    }

    @PostMapping("/{requestId}/confirm-otp")
    public ResponseEntity<RequestResponse> confirmOtp(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long requesterId,
            @Valid @RequestBody OtpConfirmRequest dto) {
        return ResponseEntity.ok(requestService.confirmOtp(requestId, requesterId, dto));
    }

    @PatchMapping("/{requestId}/fulfill")
    public ResponseEntity<RequestResponse> markFulfilled(@PathVariable Long requestId) {
        return ResponseEntity.ok(requestService.markFulfilled(requestId));
    }

    @PutMapping("/{requestId}")
    public ResponseEntity<RequestResponse> adminUpdate(
            @PathVariable Long requestId,
            @RequestBody AdminRequestUpdate dto) {
        return ResponseEntity.ok(requestService.adminUpdate(requestId, dto));
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> adminDelete(@PathVariable Long requestId) {
        requestService.adminDelete(requestId);
        return ResponseEntity.noContent().build();
    }
}
