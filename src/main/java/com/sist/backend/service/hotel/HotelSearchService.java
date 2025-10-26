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

import com.sist.backend.repository.hotel.hotelSearchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelSearchService {
    
    private final HotelInfoRepository hotelInfoRepository;
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

    /**
     * areaCode와 좌표를 기반으로 가장 근접한 호텔 목록 조회
     * @param areaCode 지역 코드 (예: "1"=서울, "31"=경기)
     * @param limit 조회할 최대 호텔 수 (기본값: 10)
     * @param lat 기준 위도
     * @param lng 기준 경도
     * @return HotelcardResponse 리스트 (거리순 정렬)
     */
    public List<HotelcardResponse> findHotelsByAreaCodeWithCoordinates(String areaCode, int limit, double lat, double lng) {
        // 해당 지역의 모든 호텔을 조회 (limit보다 많이 조회해서 거리 계산 후 정렬)
        Pageable pageable = PageRequest.of(0, limit * 3); // 더 많이 조회
        List<HotelInfo> hotels = hotelInfoRepository.findByAreaCodeWithCategoryAndArea(areaCode, pageable);
        
        // 거리 계산 및 정렬
        List<HotelcardResponse> hotelResponses = hotels.stream().map(hotel -> {
            // 해당 호텔의 객실 가격 범위 조회
            List<BigDecimal> prices = roomRepository.findBasePricesByContentId(hotel.getContentId());
            
            BigDecimal minPrice = null;
            BigDecimal maxPrice = null;
            
            if (!prices.isEmpty()) {
                minPrice = prices.stream().min(BigDecimal::compareTo).orElse(null);
                maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(null);
            }
            
            // 거리 계산 (하버사인 공식 사용)
            double distance = 0.0;
            if (hotel.getHotelLocation() != null && 
                hotel.getHotelLocation().getMapY() != null && 
                hotel.getHotelLocation().getMapX() != null) {
                distance = calculateDistance(
                    lat, 
                    lng, 
                    hotel.getHotelLocation().getMapY().doubleValue(), 
                    hotel.getHotelLocation().getMapX().doubleValue()
                );
            }
            
            return HotelcardResponse.builder()
                    .contentId(hotel.getContentId())
                    .title(hotel.getTitle())
                    .adress(hotel.getAdress())
                    .imageUrl(hotel.getImageUrl())
                    .areaCode(hotel.getAreaCode())
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .distance(distance) // 거리 정보 추가
                    .build();
        }).sorted((h1, h2) -> Double.compare(h1.getDistance(), h2.getDistance())) // 거리순 정렬
          .limit(limit) // 최종 limit 적용
          .collect(Collectors.toList());
        
        return hotelResponses;
    }

    /**
     * 두 좌표 간의 거리 계산 (하버사인 공식)
     * @param lat1 첫 번째 위도
     * @param lng1 첫 번째 경도
     * @param lat2 두 번째 위도
     * @param lng2 두 번째 경도
     * @return 거리 (킬로미터)
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // 지구 반지름 (킬로미터)
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

}
