package com.sist.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.RegistrationRequest;

@Repository
public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Integer> {

    /* 대시보드에 사용되는 승인요청 목록 (5개씩만 보여줌)*/
    /* 승인요청이 가장 오래된 순서대로 보여주기 */
    @Query("SELECT rr FROM RegistrationRequest rr " +
        "LEFT JOIN FETCH rr.admin " +
        "LEFT JOIN FETCH rr.hotelInfo " +
        "WHERE rr.status = 0 " +
        "ORDER BY rr.regiDate ASC")
    List<RegistrationRequest> findByStatusInDashboard();

    /* status가 0인 등록 요청 (페이징 처리) */
    @Query("SELECT rr FROM RegistrationRequest rr " +
        "LEFT JOIN FETCH rr.admin " +
        "LEFT JOIN FETCH rr.hotelInfo " +
        "WHERE rr.status = 0 " +
        "ORDER BY rr.regiDate ASC")
    Page<RegistrationRequest> findByStatus(Pageable pageable);
}
