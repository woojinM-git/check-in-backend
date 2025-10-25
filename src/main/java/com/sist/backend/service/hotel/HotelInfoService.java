package com.sist.backend.service.hotel;

import com.sist.backend.dto.master.HotelInfoDto;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    /* 대시보드 - 등록되어 있는 호텔 갯수 */
    public Integer findRegistrationHotelCount() {
        return hotelInfoRepository.findRegistrationHotelCount();
    }

    public Page<HotelInfoDto> findAllHotelWithDetailsAsDto(Pageable pageable) {
        Page<HotelInfo> hotelInfoPage = hotelInfoRepository.findAllHotelWithDetailsAsDto(pageable);
        return hotelInfoPage.map(HotelInfoDto::hotelInfoDto);
    }

    public Optional<String> findContentIdByAdminIdx(Integer adminIdx) {
        String contentId = hotelInfoRepository.findContentIdByAdminIdx(adminIdx);
        return Optional.ofNullable(contentId);
    }
}

