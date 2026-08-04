package com.globallogic.bloodbridge.matching.repository;

import com.globallogic.bloodbridge.matching.model.Match;
import com.globallogic.bloodbridge.matching.model.ResponseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByRequestIdOrderByMatchScoreDesc(Long requestId);

    /** Prefer this over a unique Optional finder — legacy duplicates can exist before the unique constraint. */
    List<Match> findByRequestIdAndDonorIdOrderByCreatedAtDesc(Long requestId, Long donorId);

    Optional<Match> findFirstByRequestIdAndDonorIdOrderByCreatedAtDesc(Long requestId, Long donorId);

    Optional<Match> findFirstByRequestIdAndResponseStatus(Long requestId, ResponseStatus responseStatus);

    List<Match> findByDonorIdOrderByCreatedAtDesc(Long donorId);

    List<Match> findByDonorIdAndResponseStatusOrderByCreatedAtDesc(Long donorId, ResponseStatus responseStatus);

    boolean existsByRequestIdAndDonorId(Long requestId, Long donorId);
}
