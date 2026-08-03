package com.globallogic.bloodbridge.analytics.repository;

import com.globallogic.bloodbridge.analytics.model.RequestMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestMetricRepository extends JpaRepository<RequestMetric, Long> {

    List<RequestMetric> findByRequestCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<RequestMetric> findByCityIgnoreCaseAndRequestCreatedAtBetween(String city, LocalDateTime from, LocalDateTime to);
}
