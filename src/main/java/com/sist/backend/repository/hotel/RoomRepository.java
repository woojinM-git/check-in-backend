package com.sist.backend.repository.hotel;

import com.sist.backend.entity.Room;
import com.sist.backend.entity.RoomId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, RoomId> {
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
}
