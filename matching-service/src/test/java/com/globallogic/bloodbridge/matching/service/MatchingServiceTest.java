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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPersistenceService matchPersistenceService;

    @Mock
    private DonorSearchService donorSearchService;

    @Mock
    private DonorServiceClient donorServiceClient;

    @Mock
    private RequestServiceClient requestServiceClient;

    @Mock
    private MatchEventPublisher eventPublisher;

    @InjectMocks
    private MatchingService matchingService;

    private RequestDto request;

    @BeforeEach
    void setUp() {
        request = RequestDto.builder()
                .requestId(1L).bloodGroup("B+").city("Delhi")
                .latitude(28.6129).longitude(77.2295).urgency("CRITICAL").status("PENDING")
                .build();
        lenient().when(donorServiceClient.getDonor(anyLong())).thenAnswer(inv ->
                DonorDto.builder()
                        .donorId(inv.getArgument(0))
                        .name("Donor " + inv.getArgument(0))
                        .bloodGroup("B+")
                        .city("Delhi")
                        .build());
        lenient().when(matchRepository.findFirstByRequestIdAndResponseStatus(anyLong(), any()))
                .thenReturn(Optional.empty());
        lenient().when(matchRepository.findByRequestIdOrderByMatchScoreDesc(anyLong()))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("US-003 AC2: Donors closer to the hospital are ranked higher and matched first")
    void testMatchDonors_RanksNearestDonorFirst() {
        DonorDto near = DonorDto.builder().donorId(10L).bloodGroup("B+").latitude(28.6139).longitude(77.2090).isAvailable(true).build();
        DonorDto far = DonorDto.builder().donorId(11L).bloodGroup("B+").latitude(19.0760).longitude(72.8777).isAvailable(true).build();

        when(requestServiceClient.getRequest(1L)).thenReturn(request);
        when(donorSearchService.searchEligibleDonors("B+", "Delhi")).thenReturn(List.of(far, near));
        when(matchPersistenceService.insertIfAbsent(any(Match.class))).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            m.setMatchId(m.getDonorId() != null && m.getDonorId() == 10L ? 100L : 101L);
            return Optional.of(m);
        });

        MatchResponse response = matchingService.matchDonors(1L);

        assertThat(response.getDonorId()).isEqualTo(10L);
        verify(matchPersistenceService, times(2)).insertIfAbsent(any(Match.class));
        verify(eventPublisher, times(2)).publishDonorMatched(any());
        verify(requestServiceClient).updateStatus(1L, new StatusUpdateRequest("MATCHED", null));
    }

    @Test
    @DisplayName("Duplicate donorIds from search create only one match row")
    void testMatchDonors_DedupesDuplicateDonorCandidates() {
        DonorDto donor = DonorDto.builder().donorId(10L).bloodGroup("B+").city("Delhi")
                .latitude(28.6139).longitude(77.2090).isAvailable(true).build();

        when(requestServiceClient.getRequest(1L)).thenReturn(request);
        when(donorSearchService.searchEligibleDonors("B+", "Delhi")).thenReturn(List.of(donor, donor));
        when(matchPersistenceService.insertIfAbsent(any(Match.class))).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            m.setMatchId(100L);
            return Optional.of(m);
        });

        MatchResponse response = matchingService.matchDonors(1L);

        assertThat(response.getDonorId()).isEqualTo(10L);
        verify(matchPersistenceService, times(1)).insertIfAbsent(any(Match.class));
        verify(eventPublisher, times(1)).publishDonorMatched(any());
    }

    @Test
    @DisplayName("Concurrent rematch skips donors that already have a match row")
    void testMatchDonors_SkipsExistingDonorMatches() {
        DonorDto donor = DonorDto.builder().donorId(10L).bloodGroup("B+").city("Delhi")
                .latitude(28.6139).longitude(77.2090).isAvailable(true).build();
        Match existing = Match.builder().matchId(50L).requestId(1L).donorId(10L)
                .matchScore(0.0).responseStatus(ResponseStatus.PENDING).build();

        when(requestServiceClient.getRequest(1L)).thenReturn(request);
        when(donorSearchService.searchEligibleDonors("B+", "Delhi")).thenReturn(List.of(donor));
        when(matchRepository.findByRequestIdOrderByMatchScoreDesc(1L)).thenReturn(List.of(existing));
        when(matchRepository.findFirstByRequestIdAndResponseStatus(1L, ResponseStatus.PENDING))
                .thenReturn(Optional.of(existing));

        MatchResponse response = matchingService.matchDonors(1L);

        assertThat(response.getDonorId()).isEqualTo(10L);
        verify(matchPersistenceService, never()).insertIfAbsent(any(Match.class));
        verify(eventPublisher, never()).publishDonorMatched(any());
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
        when(matchRepository.findFirstByRequestIdAndDonorIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(Optional.of(pendingMatch));
        when(matchRepository.findByRequestIdAndDonorIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(List.of(pendingMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(pendingMatch);
        when(matchRepository.findByRequestIdOrderByMatchScoreDesc(1L)).thenReturn(List.of(pendingMatch));
        when(requestServiceClient.getRequest(1L)).thenReturn(request);
        when(donorServiceClient.updateAvailability(10L, new AvailabilityUpdateRequest(false)))
                .thenReturn(DonorDto.builder().donorId(10L).name("Near").build());

        MatchResponseRequest responseRequest = new MatchResponseRequest(10L, true);
        MatchResponse result = matchingService.processResponse(1L, responseRequest);

        assertThat(result.getResponseStatus()).isEqualTo(ResponseStatus.ACCEPTED);
        verify(requestServiceClient).updateStatus(1L, new StatusUpdateRequest("CONFIRMED", 10L));
        verify(donorServiceClient).updateAvailability(10L, new AvailabilityUpdateRequest(false));
    }

    @Test
    @DisplayName("Accept/decline uses newest match when duplicate rows exist")
    void testProcessResponse_Accept_WithDuplicateRows() {
        Match older = Match.builder().matchId(99L).requestId(1L).donorId(10L).responseStatus(ResponseStatus.PENDING).build();
        Match newer = Match.builder().matchId(100L).requestId(1L).donorId(10L).responseStatus(ResponseStatus.PENDING).build();
        when(matchRepository.findFirstByRequestIdAndDonorIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(Optional.of(newer));
        when(matchRepository.findByRequestIdAndDonorIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(List.of(newer, older));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchRepository.findByRequestIdOrderByMatchScoreDesc(1L)).thenReturn(List.of(newer));
        when(requestServiceClient.getRequest(1L)).thenReturn(request);
        when(donorServiceClient.updateAvailability(10L, new AvailabilityUpdateRequest(false)))
                .thenReturn(DonorDto.builder().donorId(10L).name("Near").build());

        MatchResponse result = matchingService.processResponse(1L, new MatchResponseRequest(10L, true));

        assertThat(result.getResponseStatus()).isEqualTo(ResponseStatus.ACCEPTED);
        verify(matchRepository).delete(older);
        verify(requestServiceClient).updateStatus(1L, new StatusUpdateRequest("CONFIRMED", 10L));
    }

    @Test
    @DisplayName("US-004 AC2: Donor declining triggers a search for the next best-matched donor")
    void testProcessResponse_Decline_TriesNextDonor() {
        Match pendingMatch = Match.builder().matchId(100L).requestId(1L).donorId(10L).responseStatus(ResponseStatus.PENDING).build();
        DonorDto declinedDonor = DonorDto.builder().donorId(10L).bloodGroup("B+").latitude(28.6139).longitude(77.2090).build();
        DonorDto nextDonor = DonorDto.builder().donorId(11L).bloodGroup("B+").latitude(28.7041).longitude(77.1025).build();

        when(matchRepository.findFirstByRequestIdAndDonorIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(Optional.of(pendingMatch));
        when(matchRepository.findByRequestIdAndDonorIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(List.of(pendingMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requestServiceClient.getRequest(1L)).thenReturn(request);
        when(donorSearchService.searchEligibleDonors("B+", "Delhi")).thenReturn(List.of(declinedDonor, nextDonor));
        when(matchRepository.findByRequestIdOrderByMatchScoreDesc(1L)).thenReturn(List.of(pendingMatch));
        when(matchPersistenceService.insertIfAbsent(any(Match.class))).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            m.setMatchId(101L);
            return Optional.of(m);
        });

        MatchResponseRequest responseRequest = new MatchResponseRequest(10L, false);
        MatchResponse result = matchingService.processResponse(1L, responseRequest);

        assertThat(result.getDonorId()).isEqualTo(11L);
        assertThat(pendingMatch.getResponseStatus()).isEqualTo(ResponseStatus.DECLINED);
    }

    @Test
    @DisplayName("Responding to a request/donor combination with no match on file throws MatchNotFoundException")
    void testProcessResponse_MatchNotFound() {
        when(matchRepository.findFirstByRequestIdAndDonorIdOrderByCreatedAtDesc(1L, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchingService.processResponse(1L, new MatchResponseRequest(99L, true)))
                .isInstanceOf(MatchNotFoundException.class);
    }
}
