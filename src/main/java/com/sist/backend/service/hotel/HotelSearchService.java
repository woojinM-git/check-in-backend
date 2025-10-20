package com.sist.backend.service.hotel;

import com.sist.backend.entity.HotelInfo;
import com.sist.backend.dto.hotel.PopularHotelResponse;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import com.sist.backend.repository.hotel.RoomRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelSearchService {
    
    private final HotelInfoRepository hotelInfoRepository;
    private final RoomRepository roomRepository;

    public List<HotelInfo> findAll(){
        return hotelInfoRepository.findAll();
    }
    
    public List<PopularHotelResponse> findAllPopularHotels() {
        Pageable pageable = PageRequest.of(0, 9);
        List<HotelInfo> hotels = hotelInfoRepository.findTop9WithCategoryAndArea(pageable);
        
        return hotels.stream().map(hotel -> {
            // 해당 호텔의 객실 가격 범위 조회
            List<BigDecimal> prices = roomRepository.findBasePricesByContentId(hotel.getContentId());
            
            BigDecimal minPrice = null;
            BigDecimal maxPrice = null;
            
            if (!prices.isEmpty()) {
                minPrice = prices.stream().min(BigDecimal::compareTo).orElse(null);
                maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(null);
            }
            
            return PopularHotelResponse.builder()
                    .contentId(hotel.getContentId())
                    .title(hotel.getTitle())
                    .adress(hotel.getAdress())
                    .imageUrl(hotel.getImageUrl())
                    .areaCode(hotel.getAreaCode())
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .build();
        }).collect(Collectors.toList());
    }
}
