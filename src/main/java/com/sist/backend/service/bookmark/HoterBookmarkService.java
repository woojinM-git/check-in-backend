package com.sist.backend.service.bookmark;



import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.entity.HotelBookMark;
import com.sist.backend.repository.Bookmark.HotelBookmarkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HoterBookmarkService {
    
    private final HotelBookmarkRepository hotelBookmarkRepository;

    public HotelBookMark saveHotelBookmark(HotelBookMark hotelBookMark) {
        return hotelBookmarkRepository.save(hotelBookMark);
    }

    @Transactional
    public void deleteHotelBookmark(String contentId, Integer customerIdx) {
        hotelBookmarkRepository.deleteByContentIdAndCustomerIdx(contentId, customerIdx);
    }

    public List<HotelBookMark> getHotelBookmarkList(Integer customerIdx) {
        return hotelBookmarkRepository.findAllByCustomerIdx(customerIdx);
    }

    public Page<HotelBookMark> getHotelBookmarkPagePerList(Integer customerIdx, int page, int numPerPage) {
        Pageable pageable = PageRequest.of(page, numPerPage);
        return hotelBookmarkRepository.findAllByCustomerIdx(customerIdx, pageable);
    }
}
