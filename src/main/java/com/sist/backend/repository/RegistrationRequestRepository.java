package com.sist.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.RegistrationRequest;

@Repository
public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Integer> {
    
    /* status가 0인 등록 요청 목록 */
    @Query("SELECT rr FROM RegistrationRequest rr " +
        "LEFT JOIN FETCH rr.admin " +
        "LEFT JOIN FETCH rr.hotelInfo " +
        "WHERE rr.status = 0")
    List<RegistrationRequest> findByStatus();
}
