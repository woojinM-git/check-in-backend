package com.sist.backend.repository.hotel;

import com.sist.backend.entity.HotelInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelInfoRepository extends JpaRepository<HotelInfo, String> {

    /**
     * 상위 10개 HotelInfo 조회 (Category와 Area를 Fetch Join으로 함께 조회)
     * N+1 문제 방지를 위해 fetch join 사용
     */
    @Query("SELECT h FROM HotelInfo h " +
            "LEFT JOIN FETCH h.category " +
            "LEFT JOIN FETCH h.area " +
            "ORDER BY h.contentId")
    //     나중에 데이터넣고 정렬조건 변경해야함
    List<HotelInfo> findTop9WithCategoryAndArea(Pageable pageable);

    /**
     * 페이징 처리가 필요한 경우
     */
    @Query("SELECT h FROM HotelInfo h " +
            "LEFT JOIN FETCH h.category " +
            "LEFT JOIN FETCH h.area")
    Page<HotelInfo> findAllWithCategoryAndArea(Pageable pageable);

    /* 홈페이지에 등록되어 있는 호텔의 갯수 */
    @Query("SELECT COUNT(h) FROM HotelInfo h " +
            "WHERE h.status = 0")
    int findRegistrationHotelCount();

    /* 마스터 화면용 호텔 목록 (객실 수 포함) */
    @Query("SELECT h FROM HotelInfo h " +
            "LEFT JOIN FETCH h.hotelDetail " +
            "LEFT JOIN FETCH h.admin " +
            "ORDER BY h.contentId")

    List<HotelInfo> findAllHotelWithDetails();

    /**
     * areaCode로 호텔 목록 조회 (Category, Area, HotelLocation을 Fetch Join으로 함께 조회)
     */
    @Query("SELECT h FROM HotelInfo h " +
            "LEFT JOIN FETCH h.category " +
            "LEFT JOIN FETCH h.area " +
            "LEFT JOIN FETCH h.hotelLocation " +
            "WHERE h.areaCode = :areaCode " +
            "ORDER BY h.contentId")
    List<HotelInfo> findByAreaCodeWithCategoryAndArea(String areaCode, Pageable pageable);

    /* 마스터 화면용 호텔 목록 (페이징 처리) */
    @Query("SELECT h FROM HotelInfo h " +
            "LEFT JOIN FETCH h.hotelDetail " +
            "LEFT JOIN FETCH h.admin " +
            "ORDER BY h.contentId")
    Page<HotelInfo> findAllHotelWithDetailsAsDto(Pageable pageable);

    /* 마스터 화면용 호텔 목록 (검색 기능 포함, 페이징 처리) */
    @Query("SELECT DISTINCT h FROM HotelInfo h " +
            "LEFT JOIN FETCH h.hotelDetail " +
            "LEFT JOIN FETCH h.admin " +
            "LEFT JOIN FETCH h.area " +
            "WHERE (:search IS NULL OR :search = '' OR " +
            "       LOWER(h.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "       LOWER(h.admin.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "       LOWER(h.adress) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "       h.areaCode LIKE CONCAT('%', :search, '%') OR " +
            "       (h.area IS NOT NULL AND LOWER(h.area.areaName) LIKE LOWER(CONCAT('%', :search, '%')))) " +
            "ORDER BY h.contentId")
    Page<HotelInfo> findAllHotelWithDetailsAsDtoWithSearch(@Param("search") String search, Pageable pageable);

    /**
     * adminIdx로 contentId 조회
     */
    @Query("SELECT h.contentId FROM HotelInfo h WHERE h.adminIdx = :adminIdx")
    Optional<String> findContentIdByAdminIdx(@Param("adminIdx") Integer adminIdx);
    
    /**
     * adminIdx로 contentId 리스트 조회 (여러 호텔 소유 가능)
     */
    @Query("SELECT h.contentId FROM HotelInfo h WHERE h.adminIdx = :adminIdx")
    List<String> findContentIdsByAdminIdx(@Param("adminIdx") Integer adminIdx);

    /**
     * adminIdx로 호텔 정보 조회 (HotelDetail 포함)
     */
    @Query("SELECT h FROM HotelInfo h " +
            "LEFT JOIN FETCH h.hotelDetail " +
            "LEFT JOIN FETCH h.category " +
            "LEFT JOIN FETCH h.area " +
            "WHERE h.adminIdx = :adminIdx")
    Optional<HotelInfo> findByAdminIdxWithDetails(@Param("adminIdx") Integer adminIdx);

    /**
     * 운영중인 호텔 수 조회 (status=1)
     */
    @Query("SELECT COUNT(h) FROM HotelInfo h WHERE h.status = 1")
    Long countActiveHotels();

    /**
     * 이번달 신규 호텔 수 조회 (status=1이고 이번달에 승인된 호텔)
     * RegistrationRequest의 승인일을 기준으로 조회
     * adminIdx를 기준으로 JOIN
     */
    @Query("SELECT COUNT(DISTINCT h.contentId) FROM HotelInfo h " +
            "INNER JOIN RegistrationRequest rr ON h.adminIdx = rr.adminIdx " +
            "WHERE h.status = 1 " +
            "AND rr.status = 1 " +
            "AND rr.approvDate IS NOT NULL " +
            "AND YEAR(rr.approvDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(rr.approvDate) = MONTH(CURRENT_DATE)")
    Long countNewHotelsThisMonth();

    //호텔 예약 수 count +1 증가시키는 메서드
    //예약 성공시 호출됨
    //DB에서 count+1증가시킴 (NULL인 경우 0으로 처리 후 +1)
    @Modifying
    @Query(value = "UPDATE hotelInfo SET count = COALESCE(count, 0) + 1 WHERE contentId = :contentId", nativeQuery = true)
    int increaseReservationCount(@Param("contentId") String contentId);
}

