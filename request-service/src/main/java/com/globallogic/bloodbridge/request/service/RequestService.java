package com.globallogic.bloodbridge.request.service;

import com.globallogic.bloodbridge.request.dto.RequestCreateRequest;
import com.globallogic.bloodbridge.request.dto.RequestResponse;
import com.globallogic.bloodbridge.request.dto.StatusUpdateRequest;
import com.globallogic.bloodbridge.request.exception.InvalidRequestStateException;
import com.globallogic.bloodbridge.request.exception.RequestNotFoundException;
import com.globallogic.bloodbridge.request.model.BloodRequest;
import com.globallogic.bloodbridge.request.model.RequestStatus;
import com.globallogic.bloodbridge.request.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestService {

    private static final Logger log = LoggerFactory.getLogger(RequestService.class);

    private final RequestRepository requestRepository;

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
        return toResponse(saved);
    }

    public RequestResponse getRequest(Long requestId) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
        return toResponse(request);
    }

    public List<RequestResponse> getRequestsByRequester(Long requesterId) {
        return requestRepository.findByRequesterId(requesterId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RequestResponse cancelRequest(Long requestId) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (request.getStatus() == RequestStatus.FULFILLED) {
            throw new InvalidRequestStateException("Cannot cancel a request that has already been fulfilled");
        }

        request.setStatus(RequestStatus.CANCELLED);
        BloodRequest saved = requestRepository.save(request);
        log.info("Request id={} cancelled", requestId);
        return toResponse(saved);
    }

    @Transactional
    public RequestResponse updateStatus(Long requestId, StatusUpdateRequest dto) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));

        request.setStatus(dto.getStatus());
        if (dto.getConfirmedDonorId() != null) {
            request.setConfirmedDonorId(dto.getConfirmedDonorId());
        }

        BloodRequest saved = requestRepository.save(request);
        log.info("Request id={} status updated to {}", requestId, dto.getStatus());
        return toResponse(saved);
    }

    @Transactional
    public RequestResponse markFulfilled(Long requestId) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));

        request.setStatus(RequestStatus.FULFILLED);
        BloodRequest saved = requestRepository.save(request);
        log.info("Request id={} marked FULFILLED", requestId);
        return toResponse(saved);
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
                .createdAt(request.getCreatedAt())
                .build();
    }
}
