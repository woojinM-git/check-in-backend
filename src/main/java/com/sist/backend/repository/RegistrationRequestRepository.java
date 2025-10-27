package com.sist.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.RegistrationRequest;

@Repository
public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Integer> {

    /* 대시보드에 사용되는 승인요청 목록 (5개씩만 보여줌)*/
    /* 승인요청이 가장 오래된 순서대로 보여주기 */
    @Query("SELECT rr FROM RegistrationRequest rr " +
        "LEFT JOIN FETCH rr.admin " +
        "LEFT JOIN FETCH rr.hotelDraft " +
        "WHERE rr.status = 0 " +
        "ORDER BY rr.regiDate ASC")
    List<RegistrationRequest> findTop5ByStatusInDashboard(Pageable pageable);

    /* 승인요청중인 호텔의 수 */
    @Query("SELECT COUNT(rr) FROM RegistrationRequest rr " +
        "WHERE rr.status = 0")
    Integer findByStatusCount();

    /* 승인요청중인 호텔의 목록 */
    @Query("SELECT rr FROM RegistrationRequest rr " +
        "LEFT JOIN FETCH rr.admin " +
        "LEFT JOIN FETCH rr.hotelDraft " +
        "WHERE rr.status = 0 " +
        "ORDER BY rr.regiDate ASC")
    Page<RegistrationRequest> findByStatus(Pageable pageable);

    /* 오늘 승인된 호텔 수 */
    @Query(value = "SELECT COUNT(*) FROM registrationRequest WHERE status = 1 AND DATE(regiDate) = CURDATE()", nativeQuery = true)
    Integer findTodayApprovedCount();

    /* 오늘 거부된 호텔 수 */
    @Query(value = "SELECT COUNT(*) FROM registrationRequest WHERE status = 2 AND DATE(regiDate) = CURDATE()", nativeQuery = true)
    Integer findTodayRejectedCount();

    /* 승인 요청 업데이트 */
    @Modifying
    @Query("UPDATE RegistrationRequest rr SET rr.status = :status, rr.approvDate = :approvDate WHERE rr.registrationIdx = :registrationIdx")
    void updateRequest(@Param("registrationIdx") Integer registrationIdx, @Param("status") Integer status, @Param("approvDate") LocalDateTime approvDate);


    /* 거부 요청 업데이트 */
    @Modifying
    @Query("UPDATE RegistrationRequest rr SET rr.status = :status WHERE rr.registrationIdx = :registrationIdx")
    void updateRejectRequest(@Param("registrationIdx") Integer registrationIdx, @Param("status") Integer status);
}
