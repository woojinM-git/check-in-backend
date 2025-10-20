package com.sist.backend.service.hotel;

import com.sist.backend.entity.HotelInfo;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelInfoService {
    
    private final HotelInfoRepository hotelInfoRepository;
    
    /**
     * 상위 10개 호텔 정보 조회 (Category, Area 포함)
     */
    public List<HotelInfo> getTop10Hotels() {
        Pageable pageable = PageRequest.of(0, 10);
        return hotelInfoRepository.findTop9WithCategoryAndArea(pageable);
    }
    
    /**
     * 페이징 처리된 호텔 목록 조회
     */
    public Page<HotelInfo> getHotelsWithPaging(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return hotelInfoRepository.findAllWithCategoryAndArea(pageable);
    }

    /* 등록되어 있는 호텔의 목록 */
    public List<HotelInfo> findAllHotel() {
        return hotelInfoRepository.findAll();
    }

    /* 등록되어 있는 호텔의 목록 (객실 수 포함)*/
    public List<HotelInfo> findAllHotelWithDetails() {
        return hotelInfoRepository.findAllHotelWithDetails();
    }

}

