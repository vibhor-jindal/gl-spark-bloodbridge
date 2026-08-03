package com.globallogic.bloodbridge.request.service;

import com.globallogic.bloodbridge.request.dto.RequestCreateRequest;
import com.globallogic.bloodbridge.request.dto.RequestResponse;
import com.globallogic.bloodbridge.request.dto.StatusUpdateRequest;
import com.globallogic.bloodbridge.request.exception.InvalidRequestStateException;
import com.globallogic.bloodbridge.request.exception.RequestNotFoundException;
import com.globallogic.bloodbridge.request.model.BloodRequest;
import com.globallogic.bloodbridge.request.model.RequestStatus;
import com.globallogic.bloodbridge.request.model.Urgency;
import com.globallogic.bloodbridge.request.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RequestEventPublisher eventPublisher;

    @InjectMocks
    private RequestService requestService;

    private RequestCreateRequest createRequest;
    private BloodRequest savedRequest;

    @BeforeEach
    void setUp() {
        createRequest = new RequestCreateRequest(
                "Suresh Kumar", "B+", 2, "AIIMS Delhi", "Delhi", 28.6129, 77.2295, Urgency.CRITICAL);

        savedRequest = BloodRequest.builder()
                .requestId(1L)
                .requesterId(100L)
                .patientName("Suresh Kumar")
                .bloodGroup("B+")
                .unitsNeeded(2)
                .hospitalName("AIIMS Delhi")
                .city("Delhi")
                .latitude(28.6129)
                .longitude(77.2295)
                .urgency(Urgency.CRITICAL)
                .status(RequestStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("US-002 AC1: Valid request details create a request with PENDING status and a request ID")
    void testCreateRequest_Success() {
        when(requestRepository.save(any(BloodRequest.class))).thenReturn(savedRequest);

        RequestResponse response = requestService.createRequest(100L, createRequest);

        assertThat(response.getRequestId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(RequestStatus.PENDING);
        verify(requestRepository, times(1)).save(any(BloodRequest.class));
        verify(eventPublisher, times(1)).publishRequestCreated(any());
    }

    @Test
    @DisplayName("US-005 AC1: Tracking an existing request returns its current status")
    void testTrackRequestStatus() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(savedRequest));

        RequestResponse response = requestService.getRequest(1L);

        assertThat(response.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(response.getPatientName()).isEqualTo("Suresh Kumar");
    }

    @Test
    @DisplayName("Fetching an unknown request id throws RequestNotFoundException")
    void testGetRequest_NotFound() {
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.getRequest(99L))
                .isInstanceOf(RequestNotFoundException.class);
    }

    @Test
    @DisplayName("US-005 AC4: Cancelling a request sets its status to CANCELLED and publishes a status-changed event")
    void testCancelRequest() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(savedRequest));
        when(requestRepository.save(any(BloodRequest.class))).thenReturn(savedRequest);

        requestService.cancelRequest(1L);

        assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        verify(eventPublisher, times(1)).publishStatusChanged(any());
    }

    @Test
    @DisplayName("A fulfilled request cannot be cancelled")
    void testCancelRequest_AlreadyFulfilled_ThrowsException() {
        savedRequest.setStatus(RequestStatus.FULFILLED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(savedRequest));

        assertThatThrownBy(() -> requestService.cancelRequest(1L))
                .isInstanceOf(InvalidRequestStateException.class);

        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-005 AC2: Updating status with a confirmed donor makes the donor visible on the request")
    void testUpdateStatus_SetsConfirmedDonor() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(savedRequest));
        when(requestRepository.save(any(BloodRequest.class))).thenReturn(savedRequest);

        StatusUpdateRequest statusUpdate = new StatusUpdateRequest(RequestStatus.CONFIRMED, 55L);
        RequestResponse response = requestService.updateStatus(1L, statusUpdate);

        assertThat(response.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
        assertThat(response.getConfirmedDonorId()).isEqualTo(55L);
        verify(eventPublisher, times(1)).publishStatusChanged(any());
    }

    @Test
    @DisplayName("US-005 AC3: Marking a request fulfilled sets its status to FULFILLED")
    void testMarkFulfilled() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(savedRequest));
        when(requestRepository.save(any(BloodRequest.class))).thenReturn(savedRequest);

        requestService.markFulfilled(1L);

        assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.FULFILLED);
        verify(eventPublisher, times(1)).publishStatusChanged(any());
    }
}
