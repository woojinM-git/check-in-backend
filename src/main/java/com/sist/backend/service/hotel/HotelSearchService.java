package com.sist.backend.service.hotel;

import com.sist.backend.entity.HotelInfo;
import com.sist.backend.dto.hotel.HotelcardResponse;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import com.sist.backend.repository.hotel.RoomRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sist.backend.repository.hotel.HotelInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelSearchService {
    
    private final hotelSearchRepository hotelSearchRepository;
    private final RoomRepository roomRepository;

    public List<HotelInfo> findByTitle(String title){
        return hotelSearchRepository.findByTitle(title);
    }

    public List<HotelInfo> findAll(){
        return hotelSearchRepository.findAll();
    }
    
    public List<HotelcardResponse> findAllPopularHotels() {
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
            
            return HotelcardResponse.builder()
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
    /**
     * areaCode로 호텔 목록 조회 (가격 정보 포함)
     * @param areaCode 지역 코드 (예: "1"=서울, "31"=경기)
     * @param limit 조회할 최대 호텔 수 (기본값: 10)
     * @return HotelcardResponse 리스트
     */
    public List<HotelcardResponse> findHotelsByAreaCode(String areaCode, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<HotelInfo> hotels = hotelInfoRepository.findByAreaCodeWithCategoryAndArea(areaCode, pageable);
        
        return hotels.stream().map(hotel -> {
            // 해당 호텔의 객실 가격 범위 조회
            List<BigDecimal> prices = roomRepository.findBasePricesByContentId(hotel.getContentId());
            
            BigDecimal minPrice = null;
            BigDecimal maxPrice = null;
            
            if (!prices.isEmpty()) {
                minPrice = prices.stream().min(BigDecimal::compareTo).orElse(null);
                maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(null);
            }
            
            return HotelcardResponse.builder()
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
    
    public List<HotelcardResponse> findAllPopularHotels() {
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
            
            return HotelcardResponse.builder()
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
    /**
     * areaCode로 호텔 목록 조회 (가격 정보 포함)
     * @param areaCode 지역 코드 (예: "1"=서울, "31"=경기)
     * @param limit 조회할 최대 호텔 수 (기본값: 10)
     * @return HotelcardResponse 리스트
     */
    public List<HotelcardResponse> findHotelsByAreaCode(String areaCode, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<HotelInfo> hotels = hotelInfoRepository.findByAreaCodeWithCategoryAndArea(areaCode, pageable);
        
        return hotels.stream().map(hotel -> {
            // 해당 호텔의 객실 가격 범위 조회
            List<BigDecimal> prices = roomRepository.findBasePricesByContentId(hotel.getContentId());
            
            BigDecimal minPrice = null;
            BigDecimal maxPrice = null;
            
            if (!prices.isEmpty()) {
                minPrice = prices.stream().min(BigDecimal::compareTo).orElse(null);
                maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(null);
            }
            
            return HotelcardResponse.builder()
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
