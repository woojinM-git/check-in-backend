package com.sist.backend.repository.Bookmark;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.HotelBookMark;

@Repository
public interface HotelBookmarkRepository extends JpaRepository<HotelBookMark, Integer> {
    
    void deleteByContentIdAndCustomerIdx(String contentId, Integer customerIdx);

    List<HotelBookMark> findAllByCustomerIdx(Integer customerIdx);

    Page<HotelBookMark> findAllByCustomerIdx(Integer customerIdx, Pageable pageable);
}
