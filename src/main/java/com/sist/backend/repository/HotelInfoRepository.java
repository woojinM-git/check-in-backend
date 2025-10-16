package com.sist.backend.repository;

import com.sist.backend.entity.HotelInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

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
    List<HotelInfo> findTop10WithCategoryAndArea(Pageable pageable);
    
    /**
     * 페이징 처리가 필요한 경우
     */
    @Query("SELECT h FROM HotelInfo h " +
           "LEFT JOIN FETCH h.category " +
           "LEFT JOIN FETCH h.area")
    Page<HotelInfo> findAllWithCategoryAndArea(Pageable pageable);

    /* 마스터 화면 (홈페이지에 등록되어 있는 호텔의 목록) */
    List<HotelInfo> findAll();
}

