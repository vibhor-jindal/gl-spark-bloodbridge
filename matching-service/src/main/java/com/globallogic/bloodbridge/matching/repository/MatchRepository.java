package com.globallogic.bloodbridge.matching.repository;

import com.globallogic.bloodbridge.matching.model.Match;
import com.globallogic.bloodbridge.matching.model.ResponseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByRequestIdOrderByMatchScoreDesc(Long requestId);
    Optional<Match> findByRequestIdAndDonorId(Long requestId, Long donorId);
    Optional<Match> findFirstByRequestIdAndResponseStatus(Long requestId, ResponseStatus responseStatus);
}
