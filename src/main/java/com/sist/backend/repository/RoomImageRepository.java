package com.sist.backend.repository;

import com.sist.backend.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Integer> {
    
    /**
     * 특정 객실의 모든 이미지를 순서대로 조회 (활성 이미지만)
     */
    @Query("SELECT r FROM RoomImage r WHERE r.roomIdx = :roomIdx AND r.contentId = :contentId AND r.status = 1 ORDER BY r.imageOrder ASC")
    List<RoomImage> findByRoomIdxAndContentIdOrderByImageOrderAsc(
        @Param("roomIdx") Integer roomIdx,
        @Param("contentId") String contentId
    );
    
    /**
     * 특정 호텔의 모든 객실 이미지 조회 (활성 이미지만)
     */
    @Query("SELECT r FROM RoomImage r WHERE r.contentId = :contentId AND r.status = 1 ORDER BY r.roomIdx ASC, r.imageOrder ASC")
    List<RoomImage> findByContentIdOrderByRoomIdxAndImageOrder(
        @Param("contentId") String contentId
    );
    
    /**
     * 특정 객실의 이미지 개수 조회 (활성 이미지만, 최대 10개 제한 확인용)
     */
    @Query("SELECT COUNT(r) FROM RoomImage r WHERE r.roomIdx = :roomIdx AND r.contentId = :contentId AND r.status = 1")
    Long countByRoomIdxAndContentId(
        @Param("roomIdx") Integer roomIdx,
        @Param("contentId") String contentId
    );
    
    /**
     * 특정 객실의 특정 순서 이미지 조회 (활성 이미지만)
     */
    @Query("SELECT r FROM RoomImage r WHERE r.roomIdx = :roomIdx AND r.contentId = :contentId AND r.imageOrder = :imageOrder AND r.status = 1")
    Optional<RoomImage> findByRoomIdxAndContentIdAndImageOrder(
        @Param("roomIdx") Integer roomIdx,
        @Param("contentId") String contentId,
        @Param("imageOrder") Integer imageOrder
    );
    
    /**
     * 특정 객실의 모든 이미지 소프트 삭제 (status = 0으로 변경)
     * 하드 삭제 대신 소프트 삭제를 사용하므로 이 메서드는 사용하지 않음
     * @deprecated 소프트 삭제를 사용하세요
     */
    @Deprecated
    void deleteByRoomIdxAndContentId(Integer roomIdx, String contentId);
}

