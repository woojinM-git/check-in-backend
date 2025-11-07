package com.sist.backend.repository.hotel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.HotelImage;

@Repository
public interface HotelImageRepository extends JpaRepository<HotelImage, Integer> {

    /**
     * 특정 호텔의 활성 이미지 목록 조회 (최대 10개, id 오름차순)
     */
    @Query("SELECT h FROM HotelImage h WHERE h.contentId = :contentId AND h.status = 1 ORDER BY h.id ASC")
    List<HotelImage> findTop10ByContentIdOrderByIdAsc(@Param("contentId") String contentId);
}
