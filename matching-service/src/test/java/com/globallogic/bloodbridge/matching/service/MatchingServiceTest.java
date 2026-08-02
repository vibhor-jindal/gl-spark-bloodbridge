package com.globallogic.bloodbridge.matching.service;

import com.globallogic.bloodbridge.matching.client.DonorServiceClient;
import com.globallogic.bloodbridge.matching.client.RequestServiceClient;
import com.globallogic.bloodbridge.matching.dto.*;
import com.globallogic.bloodbridge.matching.exception.MatchNotFoundException;
import com.globallogic.bloodbridge.matching.exception.NoDonorsAvailableException;
import com.globallogic.bloodbridge.matching.model.Match;
import com.globallogic.bloodbridge.matching.model.ResponseStatus;
import com.globallogic.bloodbridge.matching.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private DonorSearchService donorSearchService;

    @Mock
    private DonorServiceClient donorServiceClient;

    @Mock
    private RequestServiceClient requestServiceClient;

    @InjectMocks
    private MatchingService matchingService;

    private RequestDto request;

    @BeforeEach
    void setUp() {
        request = RequestDto.builder()
                .requestId(1L).bloodGroup("B+").city("Delhi")
                .latitude(28.6129).longitude(77.2295).urgency("CRITICAL").status("PENDING")
                .build();
    }

    @Test
    @DisplayName("US-003 AC2: Donors closer to the hospital are ranked higher and matched first")
    void testMatchDonors_RanksNearestDonorFirst() {
        DonorDto near = DonorDto.builder().donorId(10L).bloodGroup("B+").latitude(28.6139).longitude(77.2090).isAvailable(true).build();
        DonorDto far = DonorDto.builder().donorId(11L).bloodGroup("B+").latitude(19.0760).longitude(72.8777).isAvailable(true).build();

        when(requestServiceClient.getRequest(1L)).thenReturn(request);
        when(donorSearchService.searchEligibleDonors("B+", "Delhi")).thenReturn(List.of(far, near));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            m.setMatchId(100L);
            return m;
        });

        MatchResponse response = matchingService.matchDonors(1L);

        assertThat(response.getDonorId()).isEqualTo(10L);
        verify(requestServiceClient).updateStatus(1L, new StatusUpdateRequest("MATCHED", null));
    }

    @Test
    @DisplayName("US-003 AC3: No eligible donors triggers requester notification path and throws NoDonorsAvailableException")
    void testMatchDonors_NoDonorsFound() {
        when(requestServiceClient.getRequest(1L)).thenReturn(request);
        when(donorSearchService.searchEligibleDonors("B+", "Delhi")).thenReturn(List.of());

        assertThatThrownBy(() -> matchingService.matchDonors(1L))
                .isInstanceOf(NoDonorsAvailableException.class);

        verify(requestServiceClient).updateStatus(1L, new StatusUpdateRequest("NO_DONORS_FOUND", null));
        verify(matchRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-004 AC1: Donor accepting confirms the request and marks the donor unavailable")
    void testProcessResponse_Accept() {
        Match pendingMatch = Match.builder().matchId(100L).requestId(1L).donorId(10L).responseStatus(ResponseStatus.PENDING).build();
        when(matchRepository.findByRequestIdAndDonorId(1L, 10L)).thenReturn(Optional.of(pendingMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(pendingMatch);

        MatchResponseRequest responseRequest = new MatchResponseRequest(10L, true);
        MatchResponse result = matchingService.processResponse(1L, responseRequest);

        assertThat(result.getResponseStatus()).isEqualTo(ResponseStatus.ACCEPTED);
        verify(requestServiceClient).updateStatus(1L, new StatusUpdateRequest("CONFIRMED", 10L));
        verify(donorServiceClient).updateAvailability(10L, new AvailabilityUpdateRequest(false));
    }

    @Test
    @DisplayName("US-004 AC2: Donor declining triggers a search for the next best-matched donor")
    void testProcessResponse_Decline_TriesNextDonor() {
        Match pendingMatch = Match.builder().matchId(100L).requestId(1L).donorId(10L).responseStatus(ResponseStatus.PENDING).build();
        DonorDto declinedDonor = DonorDto.builder().donorId(10L).bloodGroup("B+").latitude(28.6139).longitude(77.2090).build();
        DonorDto nextDonor = DonorDto.builder().donorId(11L).bloodGroup("B+").latitude(28.7041).longitude(77.1025).build();

        when(matchRepository.findByRequestIdAndDonorId(1L, 10L)).thenReturn(Optional.of(pendingMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requestServiceClient.getRequest(1L)).thenReturn(request);
        when(donorSearchService.searchEligibleDonors("B+", "Delhi")).thenReturn(List.of(declinedDonor, nextDonor));
        when(matchRepository.findByRequestIdOrderByMatchScoreDesc(1L)).thenReturn(List.of(pendingMatch));

        MatchResponseRequest responseRequest = new MatchResponseRequest(10L, false);
        MatchResponse result = matchingService.processResponse(1L, responseRequest);

        assertThat(result.getDonorId()).isEqualTo(11L);
        assertThat(pendingMatch.getResponseStatus()).isEqualTo(ResponseStatus.DECLINED);
    }

    @Test
    @DisplayName("Responding to a request/donor combination with no match on file throws MatchNotFoundException")
    void testProcessResponse_MatchNotFound() {
        when(matchRepository.findByRequestIdAndDonorId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchingService.processResponse(1L, new MatchResponseRequest(99L, true)))
                .isInstanceOf(MatchNotFoundException.class);
    }
}
