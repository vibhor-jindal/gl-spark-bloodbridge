package com.globallogic.bloodbridge.matching.service;

import com.globallogic.bloodbridge.matching.client.DonorServiceClient;
import com.globallogic.bloodbridge.matching.client.RequestServiceClient;
import com.globallogic.bloodbridge.matching.dto.*;
import com.globallogic.bloodbridge.matching.event.DonorMatchedEvent;
import com.globallogic.bloodbridge.matching.event.RequestConfirmedEvent;
import com.globallogic.bloodbridge.matching.exception.MatchNotFoundException;
import com.globallogic.bloodbridge.matching.exception.NoDonorsAvailableException;
import com.globallogic.bloodbridge.matching.model.Match;
import com.globallogic.bloodbridge.matching.model.ResponseStatus;
import com.globallogic.bloodbridge.matching.repository.MatchRepository;
import com.globallogic.bloodbridge.matching.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final MatchRepository matchRepository;
    private final DonorSearchService donorSearchService;
    private final DonorServiceClient donorServiceClient;
    private final RequestServiceClient requestServiceClient;
    private final MatchEventPublisher eventPublisher;

    @Transactional
    public MatchResponse matchDonors(Long requestId) {
        RequestDto request = requestServiceClient.getRequest(requestId);

        List<DonorDto> donors = donorSearchService.searchEligibleDonors(request.getBloodGroup(), request.getCity());

        if (donors.isEmpty()) {
            requestServiceClient.updateStatus(requestId, new StatusUpdateRequest("NO_DONORS_FOUND", null));
            throw new NoDonorsAvailableException(request.getBloodGroup(), request.getCity());
        }

        List<DonorDto> ranked = donors.stream()
                .sorted(Comparator.comparingDouble(d -> {
                    Double km = GeoUtils.distanceKm(request.getLatitude(), request.getLongitude(), d.getLatitude(), d.getLongitude());
                    return km == null ? Double.MAX_VALUE : km;
                }))
                .toList();

        DonorDto bestDonor = ranked.get(0);
        Match saved = createMatch(requestId, request, bestDonor);

        requestServiceClient.updateStatus(requestId, new StatusUpdateRequest("MATCHED", null));

        eventPublisher.publishDonorMatched(DonorMatchedEvent.builder()
                .requestId(requestId)
                .donorId(bestDonor.getDonorId())
                .donorName(bestDonor.getName())
                .donorEmail(bestDonor.getEmail())
                .donorPhone(bestDonor.getPhone())
                .bloodGroup(request.getBloodGroup())
                .hospitalName(request.getHospitalName())
                .unitsNeeded(request.getUnitsNeeded())
                .urgency(request.getUrgency())
                .build());

        log.info("Matched donor id={} to request id={} with score={}", bestDonor.getDonorId(), requestId, saved.getMatchScore());
        return toResponse(saved);
    }

    @Transactional
    public MatchResponse processResponse(Long requestId, MatchResponseRequest responseRequest) {
        Match match = matchRepository.findByRequestIdAndDonorId(requestId, responseRequest.getDonorId())
                .orElseThrow(() -> new MatchNotFoundException(requestId, responseRequest.getDonorId()));

        if (Boolean.TRUE.equals(responseRequest.getAccepted())) {
            match.setResponseStatus(ResponseStatus.ACCEPTED);
            match.setRespondedAt(LocalDateTime.now());
            Match saved = matchRepository.save(match);

            RequestDto request = requestServiceClient.getRequest(requestId);
            requestServiceClient.updateStatus(requestId, new StatusUpdateRequest("CONFIRMED", responseRequest.getDonorId()));
            DonorDto donor = donorServiceClient.updateAvailability(responseRequest.getDonorId(), new AvailabilityUpdateRequest(false));

            eventPublisher.publishRequestConfirmed(RequestConfirmedEvent.builder()
                    .requestId(requestId)
                    .requesterId(request.getRequesterId())
                    .donorId(donor.getDonorId())
                    .donorName(donor.getName())
                    .donorPhone(donor.getPhone())
                    .build());

            log.info("Donor id={} accepted request id={}", responseRequest.getDonorId(), requestId);
            return toResponse(saved);
        }

        match.setResponseStatus(ResponseStatus.DECLINED);
        match.setRespondedAt(LocalDateTime.now());
        matchRepository.save(match);
        log.info("Donor id={} declined request id={}", responseRequest.getDonorId(), requestId);

        return tryNextDonor(requestId);
    }

    private MatchResponse tryNextDonor(Long requestId) {
        RequestDto request = requestServiceClient.getRequest(requestId);
        List<DonorDto> donors = donorSearchService.searchEligibleDonors(request.getBloodGroup(), request.getCity());

        List<Long> alreadyTried = matchRepository.findByRequestIdOrderByMatchScoreDesc(requestId)
                .stream().map(Match::getDonorId).toList();

        Optional<DonorDto> nextDonor = donors.stream()
                .filter(d -> !alreadyTried.contains(d.getDonorId()))
                .sorted(Comparator.comparingDouble(d -> {
                    Double km = GeoUtils.distanceKm(request.getLatitude(), request.getLongitude(), d.getLatitude(), d.getLongitude());
                    return km == null ? Double.MAX_VALUE : km;
                }))
                .findFirst();

        if (nextDonor.isEmpty()) {
            requestServiceClient.updateStatus(requestId, new StatusUpdateRequest("NO_DONORS_FOUND", null));
            throw new NoDonorsAvailableException(request.getBloodGroup(), request.getCity());
        }

        DonorDto donor = nextDonor.get();
        Match saved = createMatch(requestId, request, donor);

        eventPublisher.publishDonorMatched(DonorMatchedEvent.builder()
                .requestId(requestId)
                .donorId(donor.getDonorId())
                .donorName(donor.getName())
                .donorEmail(donor.getEmail())
                .donorPhone(donor.getPhone())
                .bloodGroup(request.getBloodGroup())
                .hospitalName(request.getHospitalName())
                .unitsNeeded(request.getUnitsNeeded())
                .urgency(request.getUrgency())
                .build());

        log.info("Next best donor id={} matched to request id={}", donor.getDonorId(), requestId);
        return toResponse(saved);
    }

    private Match createMatch(Long requestId, RequestDto request, DonorDto donor) {
        Double km = GeoUtils.distanceKm(request.getLatitude(), request.getLongitude(), donor.getLatitude(), donor.getLongitude());
        double score = km != null ? GeoUtils.scoreFromDistance(km) : 1000.0;

        Match match = Match.builder()
                .requestId(requestId)
                .donorId(donor.getDonorId())
                .matchScore(score)
                .responseStatus(ResponseStatus.PENDING)
                .build();

        return matchRepository.save(match);
    }

    public List<MatchResponse> getMatchesForRequest(Long requestId) {
        return matchRepository.findByRequestIdOrderByMatchScoreDesc(requestId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private MatchResponse toResponse(Match match) {
        return MatchResponse.builder()
                .matchId(match.getMatchId())
                .requestId(match.getRequestId())
                .donorId(match.getDonorId())
                .matchScore(match.getMatchScore())
                .responseStatus(match.getResponseStatus())
                .createdAt(match.getCreatedAt())
                .build();
    }
}
