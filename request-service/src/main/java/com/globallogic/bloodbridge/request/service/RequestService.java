package com.globallogic.bloodbridge.request.service;

import com.globallogic.bloodbridge.request.client.DonorServiceClient;
import com.globallogic.bloodbridge.request.client.RewardsServiceClient;
import com.globallogic.bloodbridge.request.dto.*;
import com.globallogic.bloodbridge.request.event.DeliveryOtpEvent;
import com.globallogic.bloodbridge.request.event.RequestCreatedEvent;
import com.globallogic.bloodbridge.request.event.RequestStatusChangedEvent;
import com.globallogic.bloodbridge.request.exception.InvalidRequestStateException;
import com.globallogic.bloodbridge.request.exception.RequestNotFoundException;
import com.globallogic.bloodbridge.request.model.BloodRequest;
import com.globallogic.bloodbridge.request.model.FulfillmentSource;
import com.globallogic.bloodbridge.request.model.RequestStatus;
import com.globallogic.bloodbridge.request.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestService {

    private static final Logger log = LoggerFactory.getLogger(RequestService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RequestRepository requestRepository;
    private final RequestEventPublisher eventPublisher;
    private final RewardsServiceClient rewardsServiceClient;
    private final DonorServiceClient donorServiceClient;

    @Transactional
    public RequestResponse createRequest(Long requesterId, RequestCreateRequest dto) {
        BloodRequest request = BloodRequest.builder()
                .requesterId(requesterId)
                .patientName(dto.getPatientName())
                .bloodGroup(dto.getBloodGroup())
                .unitsNeeded(dto.getUnitsNeeded())
                .hospitalName(dto.getHospitalName())
                .city(dto.getCity())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .urgency(dto.getUrgency())
                .status(RequestStatus.PENDING)
                .build();

        BloodRequest saved = requestRepository.save(request);
        log.info("Created blood request id={} requesterId={} urgency={}", saved.getRequestId(), requesterId, saved.getUrgency());

        eventPublisher.publishRequestCreated(RequestCreatedEvent.builder()
                .requestId(saved.getRequestId())
                .requesterId(requesterId)
                .patientName(saved.getPatientName())
                .bloodGroup(saved.getBloodGroup())
                .city(saved.getCity())
                .hospitalName(saved.getHospitalName())
                .urgency(saved.getUrgency().name())
                .unitsNeeded(saved.getUnitsNeeded())
                .createdAt(saved.getCreatedAt())
                .build());

        return toResponse(saved);
    }

    public RequestResponse getRequest(Long requestId) {
        return toResponse(find(requestId));
    }

    public List<RequestResponse> getRequestsByRequester(Long requesterId) {
        return requestRepository.findByRequesterId(requesterId).stream().map(this::toResponse).toList();
    }

    public List<RequestResponse> listOpen(String city) {
        // Truly open only — reserved/in-progress bank work lives under listForBloodBank.
        var open = EnumSet.of(
                RequestStatus.PENDING,
                RequestStatus.MATCHED,
                RequestStatus.NO_DONORS_FOUND);
        List<BloodRequest> rows = (city == null || city.isBlank())
                ? requestRepository.findByStatusInOrderByCreatedAtDesc(open)
                : requestRepository.findByCityIgnoreCaseAndStatusInOrderByCreatedAtDesc(city.trim(), open);
        return rows.stream().map(this::toResponse).toList();
    }

    /** Active + history for the blood bank that reserved the request (donor-alerts equivalent). */
    public List<RequestResponse> listForBloodBank(Long bloodBankUserId) {
        return requestRepository.findByBloodBankUserIdOrderByCreatedAtDesc(bloodBankUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RequestResponse> listAll() {
        return requestRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public RequestResponse cancelRequest(Long requestId) {
        BloodRequest request = find(requestId);
        if (request.getStatus() == RequestStatus.FULFILLED) {
            throw new InvalidRequestStateException("Cannot cancel a request that has already been fulfilled");
        }
        request.setStatus(RequestStatus.CANCELLED);
        BloodRequest saved = requestRepository.save(request);
        publishStatusChanged(saved);
        return toResponse(saved);
    }

    @Transactional
    public RequestResponse updateStatus(Long requestId, StatusUpdateRequest dto) {
        BloodRequest request = find(requestId);
        // Matching must not clobber bank reserve / delivery / terminal states.
        if (isFulfillmentProtected(request.getStatus()) && dto.getStatus() != request.getStatus()) {
            log.warn("Ignoring status update requestId={} {} -> {} (fulfillment protected)",
                    requestId, request.getStatus(), dto.getStatus());
            return toResponse(request);
        }
        request.setStatus(dto.getStatus());
        if (dto.getConfirmedDonorId() != null) {
            request.setConfirmedDonorId(dto.getConfirmedDonorId());
            request.setFulfillmentSource(FulfillmentSource.DONOR);
            // Clear any prior bank reserve so restart/auth follows the donor path.
            request.setBloodBankUserId(null);
            request.setReservedBatchId(null);
        }
        BloodRequest saved = requestRepository.save(request);
        publishStatusChanged(saved);
        if (saved.getStatus() == RequestStatus.FULFILLED) {
            creditDonorRewards(saved);
        }
        return toResponse(saved);
    }

    private static boolean isFulfillmentProtected(RequestStatus status) {
        return status == RequestStatus.CONFIRMED
                || status == RequestStatus.BANK_RESERVED
                || status == RequestStatus.OUT_FOR_DELIVERY
                || status == RequestStatus.FULFILLED
                || status == RequestStatus.CANCELLED;
    }

    @Transactional
    public RequestResponse reserveFromBloodBank(Long requestId, BankReserveRequest dto) {
        BloodRequest request = find(requestId);
        if (request.getStatus() == RequestStatus.FULFILLED
                || request.getStatus() == RequestStatus.CANCELLED
                || request.getStatus() == RequestStatus.OUT_FOR_DELIVERY) {
            throw new InvalidRequestStateException("Request cannot be reserved in status " + request.getStatus());
        }

        request.setStatus(RequestStatus.BANK_RESERVED);
        request.setFulfillmentSource(FulfillmentSource.BLOOD_BANK);
        request.setBloodBankUserId(dto.getBloodBankUserId());
        request.setReservedBatchId(dto.getBatchId());
        request.setConfirmedDonorId(null);

        BloodRequest saved = requestRepository.save(request);
        log.info("Request id={} reserved from blood bank userId={} batchId={}",
                requestId, dto.getBloodBankUserId(), dto.getBatchId());
        publishStatusChanged(saved);
        return toResponse(saved);
    }

    @Transactional
    public RequestResponse startDelivery(Long requestId) {
        BloodRequest request = find(requestId);
        if (request.getStatus() != RequestStatus.CONFIRMED && request.getStatus() != RequestStatus.BANK_RESERVED) {
            throw new InvalidRequestStateException("Delivery can start only after donor confirm or blood-bank reserve");
        }

        request.setStatus(RequestStatus.OUT_FOR_DELIVERY);
        BloodRequest saved = issueDeliveryOtp(request);
        publishStatusChanged(saved);
        log.info("Delivery started for request id={} — OTP emailed to requester", requestId);
        return toResponse(saved);
    }

    /**
     * Re-issue OTP while still OUT_FOR_DELIVERY after the previous OTP expired.
     * Authorized for: ADMIN, the reserving blood-bank user, or the confirmed donor
     * (matched via donor-service: confirmedDonorId → donor.userId == X-User-Id).
     */
    @Transactional
    public RequestResponse restartDelivery(Long requestId, Long userId, String role) {
        BloodRequest request = find(requestId);
        if (request.getStatus() != RequestStatus.OUT_FOR_DELIVERY) {
            throw new InvalidRequestStateException("Restart delivery is only allowed while out for delivery");
        }
        if (!canRestartDelivery(request, userId, role)) {
            throw new InvalidRequestStateException("Only the reserving blood bank or confirmed donor can restart delivery");
        }
        if (!isOtpExpired(request)) {
            throw new InvalidRequestStateException("OTP has not expired yet — wait for the requester to confirm, or retry after expiry");
        }

        BloodRequest saved = issueDeliveryOtp(request);
        log.info("Delivery restarted for request id={} by userId={} — new OTP emailed", requestId, userId);
        return toResponse(saved);
    }

    private boolean canRestartDelivery(BloodRequest request, Long userId, String role) {
        if (role != null && "ADMIN".equalsIgnoreCase(role.trim())) {
            return true;
        }
        if (userId == null) {
            return false;
        }

        boolean donorFulfillment = request.getFulfillmentSource() == FulfillmentSource.DONOR
                || (request.getConfirmedDonorId() != null
                && request.getFulfillmentSource() != FulfillmentSource.BLOOD_BANK);

        if (donorFulfillment && request.getConfirmedDonorId() != null) {
            return isConfirmedDonorUser(request.getConfirmedDonorId(), userId);
        }

        return request.getBloodBankUserId() != null && request.getBloodBankUserId().equals(userId);
    }

    /** Resolve donor PK → auth userId; never compare confirmedDonorId directly to X-User-Id. */
    private boolean isConfirmedDonorUser(Long confirmedDonorId, Long userId) {
        try {
            DonorDto donor = donorServiceClient.getDonor(confirmedDonorId);
            return donor != null && donor.getUserId() != null && donor.getUserId().equals(userId);
        } catch (Exception ex) {
            log.warn("Could not resolve donor id={} for restart auth: {}", confirmedDonorId, ex.getMessage());
            return false;
        }
    }

    private static boolean isOtpExpired(BloodRequest request) {
        return request.getOtpExpiresAt() == null || !request.getOtpExpiresAt().isAfter(LocalDateTime.now());
    }

    private BloodRequest issueDeliveryOtp(BloodRequest request) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        request.setDeliveryOtp(otp);
        request.setOtpExpiresAt(LocalDateTime.now().plusMinutes(30));

        BloodRequest saved = requestRepository.save(request);

        eventPublisher.publishDeliveryOtp(DeliveryOtpEvent.builder()
                .requestId(saved.getRequestId())
                .requesterId(saved.getRequesterId())
                .otp(otp)
                .patientName(saved.getPatientName())
                .hospitalName(saved.getHospitalName())
                .bloodGroup(saved.getBloodGroup())
                .unitsNeeded(saved.getUnitsNeeded())
                .build());

        return saved;
    }

    @Transactional
    public RequestResponse confirmOtp(Long requestId, Long requesterId, OtpConfirmRequest dto) {
        BloodRequest request = find(requestId);
        if (!request.getRequesterId().equals(requesterId)) {
            throw new InvalidRequestStateException("Only the requester can confirm delivery OTP");
        }
        if (request.getStatus() != RequestStatus.OUT_FOR_DELIVERY) {
            throw new InvalidRequestStateException("OTP confirmation is only valid while blood is out for delivery");
        }
        if (request.getOtpExpiresAt() == null || request.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRequestStateException("OTP has expired — ask the donor/bank to restart delivery");
        }
        if (request.getDeliveryOtp() == null || !request.getDeliveryOtp().equals(dto.getOtp().trim())) {
            throw new InvalidRequestStateException("Invalid OTP");
        }

        request.setStatus(RequestStatus.FULFILLED);
        request.setDeliveryOtp(null);
        request.setOtpExpiresAt(null);
        BloodRequest saved = requestRepository.save(request);
        publishStatusChanged(saved);
        creditDonorRewards(saved);
        log.info("Request id={} fulfilled via OTP confirmation", requestId);
        return toResponse(saved);
    }

    @Transactional
    public RequestResponse markFulfilled(Long requestId) {
        BloodRequest request = find(requestId);
        request.setStatus(RequestStatus.FULFILLED);
        request.setDeliveryOtp(null);
        request.setOtpExpiresAt(null);
        BloodRequest saved = requestRepository.save(request);
        publishStatusChanged(saved);
        creditDonorRewards(saved);
        return toResponse(saved);
    }

    @Transactional
    public RequestResponse adminUpdate(Long requestId, AdminRequestUpdate dto) {
        BloodRequest request = find(requestId);
        if (dto.getPatientName() != null) request.setPatientName(dto.getPatientName());
        if (dto.getBloodGroup() != null) request.setBloodGroup(dto.getBloodGroup());
        if (dto.getUnitsNeeded() != null) request.setUnitsNeeded(dto.getUnitsNeeded());
        if (dto.getHospitalName() != null) request.setHospitalName(dto.getHospitalName());
        if (dto.getCity() != null) request.setCity(dto.getCity());
        if (dto.getUrgency() != null) request.setUrgency(dto.getUrgency());
        if (dto.getStatus() != null) request.setStatus(dto.getStatus());
        if (dto.getConfirmedDonorId() != null) request.setConfirmedDonorId(dto.getConfirmedDonorId());
        BloodRequest saved = requestRepository.save(request);
        publishStatusChanged(saved);
        return toResponse(saved);
    }

    @Transactional
    public void adminDelete(Long requestId) {
        if (!requestRepository.existsById(requestId)) {
            throw new RequestNotFoundException(requestId);
        }
        requestRepository.deleteById(requestId);
        log.info("Admin deleted request id={}", requestId);
    }

    private BloodRequest find(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
    }

    private void publishStatusChanged(BloodRequest request) {
        eventPublisher.publishStatusChanged(RequestStatusChangedEvent.builder()
                .requestId(request.getRequestId())
                .status(request.getStatus().name())
                .confirmedDonorId(request.getConfirmedDonorId())
                .changedAt(LocalDateTime.now())
                .build());
    }

    private void creditDonorRewards(BloodRequest request) {
        if (request.getConfirmedDonorId() == null) {
            return;
        }
        try {
            rewardsServiceClient.creditDonation(request.getConfirmedDonorId());
            log.info("Credited rewards for donor id={} (request {})", request.getConfirmedDonorId(), request.getRequestId());
        } catch (Exception ex) {
            log.warn("Could not credit rewards for donor id={}: {}", request.getConfirmedDonorId(), ex.getMessage());
        }
    }

    private RequestResponse toResponse(BloodRequest request) {
        return RequestResponse.builder()
                .requestId(request.getRequestId())
                .requesterId(request.getRequesterId())
                .patientName(request.getPatientName())
                .bloodGroup(request.getBloodGroup())
                .unitsNeeded(request.getUnitsNeeded())
                .hospitalName(request.getHospitalName())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .urgency(request.getUrgency())
                .status(request.getStatus())
                .confirmedDonorId(request.getConfirmedDonorId())
                .fulfillmentSource(request.getFulfillmentSource())
                .bloodBankUserId(request.getBloodBankUserId())
                .reservedBatchId(request.getReservedBatchId())
                .otpPending(request.getStatus() == RequestStatus.OUT_FOR_DELIVERY)
                .otpExpiresAt(request.getOtpExpiresAt())
                .otpExpired(request.getStatus() == RequestStatus.OUT_FOR_DELIVERY && isOtpExpired(request))
                .createdAt(request.getCreatedAt())
                .build();
    }
}
