package com.sist.backend.repository.hotel;

import com.sist.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    // contentId(호텔 기본키)로 객실 전체 조회
    List<Room> findByContentId(String contentId);

    // 이름 부분일치(대소문자 무시)로 객실 검색
    List<Room> findByContentIdAndNameContainingIgnoreCase(String contentId, String name);

    // 특정 호텔의 모든 객실 가격 조회
    @Query("SELECT r.basePrice FROM Room r WHERE r.contentId = :contentId AND r.basePrice IS NOT NULL")
    List<BigDecimal> findBasePricesByContentId(@Param("contentId") String contentId);

    /* room 목록(admin) */
    @Query("SELECT r FROM Room r " +
        "WHERE r.contentId = :contentId")
    List<Room> findByContentIdAdmin(@Param("contentId") String contentId);

    // roomIdx 단일 키로 조회 (복합키이지만 roomIdx로 단일 조회가 필요한 서비스용)
    Optional<Room> findByRoomIdx(Integer roomIdx);
    
    // 특정 호텔의 최대 roomIdx 조회
    @Query("SELECT COALESCE(MAX(r.roomIdx), 0) FROM Room r WHERE r.contentId = :contentId")
    Integer findMaxRoomIdxByContentId(@Param("contentId") String contentId);
}
