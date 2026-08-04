package com.globallogic.bloodbridge.request.repository;

import com.globallogic.bloodbridge.request.model.BloodRequest;
import com.globallogic.bloodbridge.request.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RequestRepository extends JpaRepository<BloodRequest, Long> {
    List<BloodRequest> findByRequesterId(Long requesterId);

    List<BloodRequest> findByBloodBankUserIdOrderByCreatedAtDesc(Long bloodBankUserId);

    List<BloodRequest> findByStatusInOrderByCreatedAtDesc(Collection<RequestStatus> statuses);

    List<BloodRequest> findByCityIgnoreCaseAndStatusInOrderByCreatedAtDesc(String city, Collection<RequestStatus> statuses);
}
