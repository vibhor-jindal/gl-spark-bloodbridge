package com.globallogic.bloodbridge.matching.service;

import com.globallogic.bloodbridge.matching.model.Match;
import com.globallogic.bloodbridge.matching.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Isolates match inserts so a unique-constraint race (Kafka auto-match + UI trigger)
 * does not mark the outer matching transaction rollback-only.
 */
@Service
@RequiredArgsConstructor
public class MatchPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(MatchPersistenceService.class);

    private final MatchRepository matchRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Match> insertIfAbsent(Match match) {
        if (matchRepository.existsByRequestIdAndDonorId(match.getRequestId(), match.getDonorId())) {
            log.debug("Skip duplicate match requestId={} donorId={}", match.getRequestId(), match.getDonorId());
            return Optional.empty();
        }
        try {
            return Optional.of(matchRepository.saveAndFlush(match));
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent match insert ignored requestId={} donorId={}",
                    match.getRequestId(), match.getDonorId());
            return Optional.empty();
        }
    }
}
