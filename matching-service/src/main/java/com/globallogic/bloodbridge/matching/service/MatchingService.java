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
import com.globallogic.bloodbridge.matching.util.CityClusters;
import com.globallogic.bloodbridge.matching.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final MatchRepository matchRepository;
    private final MatchPersistenceService matchPersistenceService;
    private final DonorSearchService donorSearchService;
    private final DonorServiceClient donorServiceClient;
    private final RequestServiceClient requestServiceClient;
    private final MatchEventPublisher eventPublisher;

    @Transactional
    public MatchResponse matchDonors(Long requestId) {
        RequestDto request = requestServiceClient.getRequest(requestId);

        List<DonorDto> donors = dedupeByDonorId(
                donorSearchService.searchEligibleDonors(request.getBloodGroup(), request.getCity()));

        Set<Long> alreadyTried = matchRepository.findByRequestIdOrderByMatchScoreDesc(requestId)
                .stream()
                .map(Match::getDonorId)
                .collect(Collectors.toSet());

        List<DonorDto> candidates = donors.stream()
                .filter(d -> !alreadyTried.contains(d.getDonorId()))
                .sorted(sameCityThenDistance(request))
                .toList();

        Optional<Match> existingPending = matchRepository.findFirstByRequestIdAndResponseStatus(requestId, ResponseStatus.PENDING);

        // Rematch / first match: alert every new eligible donor (not only the nearest).
        if (candidates.isEmpty()) {
            if (existingPending.isPresent()) {
                return toResponse(existingPending.get());
            }
            requestServiceClient.updateStatus(requestId, new StatusUpdateRequest("NO_DONORS_FOUND", null));
            throw new NoDonorsAvailableException(request.getBloodGroup(), request.getCity());
        }

        List<Match> created = new ArrayList<>();
        for (DonorDto donor : candidates) {
            Match saved = createMatchIfAbsent(requestId, request, donor);
            if (saved == null) {
                continue;
            }
            created.add(saved);
            eventPublisher.publishDonorMatched(DonorMatchedEvent.builder()
                    .requestId(requestId)
                    .donorId(donor.getDonorId())
                    .donorUserId(donor.getUserId())
                    .donorName(donor.getName())
                    .donorEmail(donor.getEmail())
                    .donorPhone(donor.getPhone())
                    .bloodGroup(request.getBloodGroup())
                    .hospitalName(request.getHospitalName())
                    .unitsNeeded(request.getUnitsNeeded())
                    .urgency(request.getUrgency())
                    .build());
            log.info("Matched donor id={} to request id={} with score={}", donor.getDonorId(), requestId, saved.getMatchScore());
        }

        if (created.isEmpty()) {
            Optional<Match> pending = matchRepository.findFirstByRequestIdAndResponseStatus(requestId, ResponseStatus.PENDING);
            if (pending.isPresent()) {
                return toResponse(pending.get());
            }
            List<Match> existing = matchRepository.findByRequestIdOrderByMatchScoreDesc(requestId);
            if (!existing.isEmpty()) {
                requestServiceClient.updateStatus(requestId, new StatusUpdateRequest("MATCHED", null));
                return toResponse(existing.get(0));
            }
            requestServiceClient.updateStatus(requestId, new StatusUpdateRequest("NO_DONORS_FOUND", null));
            throw new NoDonorsAvailableException(request.getBloodGroup(), request.getCity());
        }

        requestServiceClient.updateStatus(requestId, new StatusUpdateRequest("MATCHED", null));
        return toResponse(created.get(0));
    }

    @Transactional
    public MatchResponse processResponse(Long requestId, MatchResponseRequest responseRequest) {
        Match match = resolveMatch(requestId, responseRequest.getDonorId());
        // Collapse any legacy duplicate rows for this pair so unique lookups stay safe.
        collapseDuplicateMatches(requestId, responseRequest.getDonorId(), match);

        if (Boolean.TRUE.equals(responseRequest.getAccepted())) {
            match.setResponseStatus(ResponseStatus.ACCEPTED);
            match.setRespondedAt(LocalDateTime.now());
            Match saved = matchRepository.save(match);

            // Close other open offers for this request.
            matchRepository.findByRequestIdOrderByMatchScoreDesc(requestId).stream()
                    .filter(m -> !m.getMatchId().equals(saved.getMatchId()))
                    .filter(m -> m.getResponseStatus() == ResponseStatus.PENDING)
                    .forEach(m -> {
                        m.setResponseStatus(ResponseStatus.DECLINED);
                        m.setRespondedAt(LocalDateTime.now());
                        matchRepository.save(m);
                    });

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

        Optional<Match> otherPending = matchRepository.findFirstByRequestIdAndResponseStatus(requestId, ResponseStatus.PENDING);
        if (otherPending.isPresent()) {
            return toResponse(otherPending.get());
        }

        return tryNextDonor(requestId);
    }

    private MatchResponse tryNextDonor(Long requestId) {
        RequestDto request = requestServiceClient.getRequest(requestId);
        List<DonorDto> donors = dedupeByDonorId(
                donorSearchService.searchEligibleDonors(request.getBloodGroup(), request.getCity()));

        Set<Long> alreadyTried = matchRepository.findByRequestIdOrderByMatchScoreDesc(requestId)
                .stream()
                .map(Match::getDonorId)
                .collect(Collectors.toSet());

        Optional<DonorDto> nextDonor = donors.stream()
                .filter(d -> !alreadyTried.contains(d.getDonorId()))
                .sorted(sameCityThenDistance(request))
                .findFirst();

        if (nextDonor.isEmpty()) {
            requestServiceClient.updateStatus(requestId, new StatusUpdateRequest("NO_DONORS_FOUND", null));
            throw new NoDonorsAvailableException(request.getBloodGroup(), request.getCity());
        }

        DonorDto donor = nextDonor.get();
        Match saved = createMatchIfAbsent(requestId, request, donor);
        if (saved == null) {
            return toResponse(resolveMatch(requestId, donor.getDonorId()));
        }

        eventPublisher.publishDonorMatched(DonorMatchedEvent.builder()
                .requestId(requestId)
                .donorId(donor.getDonorId())
                .donorUserId(donor.getUserId())
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

    private static Comparator<DonorDto> sameCityThenDistance(RequestDto request) {
        return Comparator
                .comparing((DonorDto d) -> CityClusters.isSameCity(request.getCity(), d.getCity()) ? 0 : 1)
                .thenComparingDouble(d -> {
                    Double km = GeoUtils.distanceKm(
                            request.getLatitude(), request.getLongitude(), d.getLatitude(), d.getLongitude());
                    return km == null ? Double.MAX_VALUE : km;
                });
    }

    /** Returns null when a match for this pair already exists (idempotent rematch / race). */
    private Match createMatchIfAbsent(Long requestId, RequestDto request, DonorDto donor) {
        Double km = GeoUtils.distanceKm(request.getLatitude(), request.getLongitude(), donor.getLatitude(), donor.getLongitude());
        // 0 = city-only / no geo — do not default to 1000 (looks like reward points in the UI).
        double score = km != null ? GeoUtils.scoreFromDistance(km) : 0.0;

        Match match = Match.builder()
                .requestId(requestId)
                .donorId(donor.getDonorId())
                .matchScore(score)
                .responseStatus(ResponseStatus.PENDING)
                .build();

        return matchPersistenceService.insertIfAbsent(match).orElse(null);
    }

    private Match resolveMatch(Long requestId, Long donorId) {
        return matchRepository.findFirstByRequestIdAndDonorIdOrderByCreatedAtDesc(requestId, donorId)
                .orElseThrow(() -> new MatchNotFoundException(requestId, donorId));
    }

    /** Keep the newest row; decline+delete older duplicates for the same request/donor pair. */
    private void collapseDuplicateMatches(Long requestId, Long donorId, Match keeper) {
        List<Match> all = matchRepository.findByRequestIdAndDonorIdOrderByCreatedAtDesc(requestId, donorId);
        if (all.size() <= 1) {
            return;
        }
        for (Match extra : all) {
            if (extra.getMatchId().equals(keeper.getMatchId())) {
                continue;
            }
            if (extra.getResponseStatus() == ResponseStatus.PENDING) {
                extra.setResponseStatus(ResponseStatus.DECLINED);
                extra.setRespondedAt(LocalDateTime.now());
                matchRepository.save(extra);
            }
            matchRepository.delete(extra);
            log.info("Removed duplicate match id={} for requestId={} donorId={}",
                    extra.getMatchId(), requestId, donorId);
        }
    }

    /** Prefer same-city / first-seen when donor search returns the same donorId more than once. */
    static List<DonorDto> dedupeByDonorId(List<DonorDto> donors) {
        if (donors == null || donors.isEmpty()) {
            return List.of();
        }
        Map<Long, DonorDto> byId = new LinkedHashMap<>();
        for (DonorDto d : donors) {
            if (d == null || d.getDonorId() == null) {
                continue;
            }
            byId.putIfAbsent(d.getDonorId(), d);
        }
        return new ArrayList<>(byId.values());
    }

    public List<MatchResponse> getMatchesForRequest(Long requestId) {
        return dedupeMatchResponses(
                matchRepository.findByRequestIdOrderByMatchScoreDesc(requestId)
                        .stream()
                        .map(this::toResponse)
                        .toList());
    }

    public List<MatchResponse> getMatchesForDonor(Long donorId) {
        return dedupeMatchResponses(
                matchRepository.findByDonorIdOrderByCreatedAtDesc(donorId)
                        .stream()
                        .map(this::toResponse)
                        .toList());
    }

    public List<MatchResponse> getPendingMatchesForDonor(Long donorId) {
        return dedupeMatchResponses(
                matchRepository.findByDonorIdAndResponseStatusOrderByCreatedAtDesc(donorId, ResponseStatus.PENDING)
                        .stream()
                        .map(this::toResponse)
                        .toList());
    }

    /** One row per requestId+donorId (keep highest score / first in list order). */
    static List<MatchResponse> dedupeMatchResponses(List<MatchResponse> matches) {
        Map<String, MatchResponse> unique = new LinkedHashMap<>();
        for (MatchResponse m : matches) {
            String key = m.getRequestId() + ":" + m.getDonorId();
            unique.putIfAbsent(key, m);
        }
        return new ArrayList<>(unique.values());
    }

    private MatchResponse toResponse(Match match) {
        String donorName = null;
        String donorBloodGroup = null;
        String donorCity = null;
        String donorPhone = null;
        try {
            DonorDto donor = donorServiceClient.getDonor(match.getDonorId());
            donorName = donor.getName();
            donorBloodGroup = donor.getBloodGroup();
            donorCity = donor.getCity();
            donorPhone = donor.getPhone();
        } catch (Exception ex) {
            log.warn("Could not enrich match {} with donor {}: {}", match.getMatchId(), match.getDonorId(), ex.getMessage());
        }

        return MatchResponse.builder()
                .matchId(match.getMatchId())
                .requestId(match.getRequestId())
                .donorId(match.getDonorId())
                .donorName(donorName)
                .donorBloodGroup(donorBloodGroup)
                .donorCity(donorCity)
                .donorPhone(donorPhone)
                .matchScore(match.getMatchScore())
                .responseStatus(match.getResponseStatus())
                .createdAt(match.getCreatedAt())
                .build();
    }
}
