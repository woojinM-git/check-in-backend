package com.sist.backend.service.hotel;

import com.sist.backend.entity.HotelInfo;
import com.sist.backend.dto.hotel.HotelcardResponse;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import com.sist.backend.repository.hotel.HotelLocationRepository;
import com.sist.backend.repository.hotel.RoomRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final HotelLocationRepository hotelLocationRepository;

    public List<HotelInfo> findByTitle(String title){
        // 검색어 유효성 검사 및 공백 제거
        if (title == null || title.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String trimmedTitle = title.trim();
        // 최소 2글자 이상 검증
        if (trimmedTitle.length() < 2) {
            return new ArrayList<>();
        }
        
        // 검색 패턴 생성 (%검색어%)
        String searchPattern = "%" + trimmedTitle + "%";
        
        System.out.println("=== findByTitle 디버깅 ===");
        System.out.println("원본 검색어: " + title);
        System.out.println("검색 패턴: " + searchPattern);
        
        // 페이지네이션 없이 전체 조회 (기존 호환성을 위해 유지)
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<HotelInfo> hotelPage = hotelSearchRepository.findByTitleWithPagination(searchPattern, null, pageable);
        List<HotelInfo> hotels = hotelPage.getContent();
        
        // 각 호텔에 hotelLocation 정보 추가
        for (HotelInfo hotel : hotels) {
            hotelLocationRepository.findAll().stream()
                .filter(loc -> hotel.getContentId().equals(loc.getContentId()))
                .findFirst()
                .ifPresent(hotel::setHotelLocation);
        }
        
        return hotels;
    }

    /**
     * 제목으로 호텔 검색 (가격 정보 포함)
     * @param title 검색할 호텔 제목
     * @param hasDining 다이닝이 있는 호텔만 필터링 (null이면 필터링 안 함)
     * @param pageable 페이지네이션 정보
     * @return HotelcardResponse 페이지 (가격 정보 포함)
     */
    public Page<HotelcardResponse> findByTitleWithPrice(String title, Boolean hasDining, Pageable pageable){
        System.out.println("=== findByTitleWithPrice 시작 ===");
        System.out.println("받은 title 파라미터: [" + title + "]");
        System.out.println("title == null: " + (title == null));
        if (title != null) {
            System.out.println("title.isEmpty(): " + title.isEmpty());
            System.out.println("title.trim().isEmpty(): " + title.trim().isEmpty());
        }
        
        // 검색어 유효성 검사 및 공백 제거
        if (title == null || title.trim().isEmpty()) {
            System.out.println("⚠️ title이 null이거나 빈 문자열입니다. 빈 페이지 반환");
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
        
        String trimmedTitle = title.trim();
        // 최소 2글자 이상 검증
        if (trimmedTitle.length() < 2) {
            System.out.println("⚠️ 검색어가 2글자 미만입니다. 빈 페이지 반환");
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
        
        // 검색 패턴 생성 (%검색어%)
        String searchPattern = "%" + trimmedTitle + "%";
        
        System.out.println("=== 검색 디버깅 ===");
        System.out.println("원본 검색어: [" + title + "]");
        System.out.println("공백 제거 후: [" + trimmedTitle + "]");
        System.out.println("검색 패턴: [" + searchPattern + "]");
        System.out.println("다이닝 필터링: " + (hasDining != null && hasDining ? "활성화" : "비활성화"));
        System.out.println("검색 패턴 길이: " + searchPattern.length());
        System.out.println("검색 패턴 바이트: " + java.util.Arrays.toString(searchPattern.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        
        System.out.println("Repository 호출 전...");
        Page<HotelInfo> hotelPage = hotelSearchRepository.findByTitleWithPagination(searchPattern, hasDining, pageable);
        System.out.println("Repository 호출 완료!");
        
        System.out.println("검색 결과 개수: " + (hotelPage != null ? hotelPage.getTotalElements() : 0));
        
        List<HotelcardResponse> hotelResponses = hotelPage.getContent().stream().map(hotel -> {
            // 해당 호텔의 객실 가격 범위 조회
            List<BigDecimal> prices = roomRepository.findBasePricesByContentId(hotel.getContentId());
            
            BigDecimal minPrice = null;
            BigDecimal maxPrice = null;
            
            if (!prices.isEmpty()) {
                minPrice = prices.stream().min(BigDecimal::compareTo).orElse(null);
                maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(null);
            }
            
            // 호텔 위치 정보 조회
            BigDecimal mapX = hotelLocationRepository.findAll().stream()
                .filter(loc -> hotel.getContentId().equals(loc.getContentId()))
                .findFirst()
                .map(location -> location.getMapX())
                .orElse(null);
            BigDecimal mapY = hotelLocationRepository.findAll().stream()
                .filter(loc -> hotel.getContentId().equals(loc.getContentId()))
                .findFirst()
                .map(location -> location.getMapY())
                .orElse(null);
            
            // 호텔 상세 정보 조회 (hotelDetail)
            String foodplace = null;
            String parkinglodging = null;
            if (hotel.getHotelDetail() != null) {
                foodplace = hotel.getHotelDetail().getFoodplace();
                parkinglodging = hotel.getHotelDetail().getParkinglodging();
            }
            
            return HotelcardResponse.builder()
                    .contentId(hotel.getContentId())
                    .title(hotel.getTitle())
                    .adress(hotel.getAdress())
                    .imageUrl(hotel.getImageUrl())
                    .areaCode(hotel.getAreaCode())
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .mapX(mapX)
                    .mapY(mapY)
                    .foodplace(foodplace)
                    .parkinglodging(parkinglodging)
                    .build();
        }).collect(Collectors.toList());
        
        return new PageImpl<>(hotelResponses, pageable, hotelPage.getTotalElements());
    }

    public List<HotelInfo> findAll(){
        return hotelSearchRepository.findAll();
    }

    /**
     * 모든 호텔 조회 (가격 정보 포함)
     * @param pageable 페이지네이션 정보
     * @return HotelcardResponse 페이지 (가격 정보 포함)
     */
    public Page<HotelcardResponse> findAllWithPrice(Pageable pageable){
        Page<HotelInfo> hotelPage = hotelSearchRepository.findAll(pageable);
        
        List<HotelcardResponse> hotelResponses = hotelPage.getContent().stream().map(hotel -> {
            // 해당 호텔의 객실 가격 범위 조회
            List<BigDecimal> prices = roomRepository.findBasePricesByContentId(hotel.getContentId());
            
            BigDecimal minPrice = null;
            BigDecimal maxPrice = null;
            
            if (!prices.isEmpty()) {
                minPrice = prices.stream().min(BigDecimal::compareTo).orElse(null);
                maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(null);
            }
            
            // 호텔 위치 정보 조회
            BigDecimal mapX = hotelLocationRepository.findAll().stream()
                .filter(loc -> hotel.getContentId().equals(loc.getContentId()))
                .findFirst()
                .map(location -> location.getMapX())
                .orElse(null);
            BigDecimal mapY = hotelLocationRepository.findAll().stream()
                .filter(loc -> hotel.getContentId().equals(loc.getContentId()))
                .findFirst()
                .map(location -> location.getMapY())
                .orElse(null);
            
            // 호텔 상세 정보 조회 (hotelDetail)
            String foodplace = null;
            String parkinglodging = null;
            if (hotel.getHotelDetail() != null) {
                foodplace = hotel.getHotelDetail().getFoodplace();
                parkinglodging = hotel.getHotelDetail().getParkinglodging();
            }
            
            return HotelcardResponse.builder()
                    .contentId(hotel.getContentId())
                    .title(hotel.getTitle())
                    .adress(hotel.getAdress())
                    .imageUrl(hotel.getImageUrl())
                    .areaCode(hotel.getAreaCode())
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .mapX(mapX)
                    .mapY(mapY)
                    .foodplace(foodplace)
                    .parkinglodging(parkinglodging)
                    .build();
        }).collect(Collectors.toList());
        
        return new PageImpl<>(hotelResponses, pageable, hotelPage.getTotalElements());
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
            
            // 호텔 상세 정보 조회 (hotelDetail)
            String foodplace = null;
            String parkinglodging = null;
            if (hotel.getHotelDetail() != null) {
                foodplace = hotel.getHotelDetail().getFoodplace();
                parkinglodging = hotel.getHotelDetail().getParkinglodging();
            }
            
            return HotelcardResponse.builder()
                    .contentId(hotel.getContentId())
                    .title(hotel.getTitle())
                    .adress(hotel.getAdress())
                    .imageUrl(hotel.getImageUrl())
                    .areaCode(hotel.getAreaCode())
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .foodplace(foodplace)
                    .parkinglodging(parkinglodging)
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
            
            // 호텔 상세 정보 조회 (hotelDetail)
            String foodplace = null;
            String parkinglodging = null;
            if (hotel.getHotelDetail() != null) {
                foodplace = hotel.getHotelDetail().getFoodplace();
                parkinglodging = hotel.getHotelDetail().getParkinglodging();
            }
            
            return HotelcardResponse.builder()
                    .contentId(hotel.getContentId())
                    .title(hotel.getTitle())
                    .adress(hotel.getAdress())
                    .imageUrl(hotel.getImageUrl())
                    .areaCode(hotel.getAreaCode())
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .foodplace(foodplace)
                    .parkinglodging(parkinglodging)
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
            
            // 호텔 상세 정보 조회 (hotelDetail)
            String foodplace = null;
            String parkinglodging = null;
            if (hotel.getHotelDetail() != null) {
                foodplace = hotel.getHotelDetail().getFoodplace();
                parkinglodging = hotel.getHotelDetail().getParkinglodging();
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
                    .foodplace(foodplace)
                    .parkinglodging(parkinglodging)
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
