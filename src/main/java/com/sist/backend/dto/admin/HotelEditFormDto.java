package com.sist.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 호텔 수정 폼 DTO (Entity 필드명과 일치)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelEditFormDto {
    // 기본 정보 (HotelInfo Entity 필드명)
    private HotelInfoDto hotelInfo;
    
    // 호텔 상세 정보 (HotelDetail Entity 필드명)
    private HotelDetailDto hotelDetail;
    
    // 지역 정보 (Area 관련)
    private AreaDto area;
    
    // 이미지 정보 (HotelImage Entity 필드명)
    private List<ImageDto> images;
    
    // 객실 정보 (Room Entity 필드명)
    private List<RoomDto> rooms;
    
    // 다이닝 정보 (Dining Entity 필드명)
    private List<DiningDto> dining;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HotelInfoDto {
        private String title;          // HotelInfo.title
        private String adress;          // HotelInfo.adress (기존 필드, 하위 호환성 유지)
        private String baseAddress;     // 도로명 주소 (신규 필드)
        private String detailAddress;   // 상세 주소 (신규 필드)
        private String tel;             // HotelInfo.tel (기존 phone → tel로 변경)
        private String imageUrl;        // HotelInfo.imageUrl (대표 이미지 URL)
        private String latitude;        // 위도 (mapY) - HotelLocation.mapY
        private String longitude;       // 경도 (mapX) - HotelLocation.mapX
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HotelDetailDto {
        private String reservationlodging;  // HotelDetail.reservationlodging (호텔 소개)
        private String foodplace;            // HotelDetail.foodplace (식당 정보)
        private String scalelodging;        // HotelDetail.scalelodging (호텔 규모)
        private String parkinglodging;     // HotelDetail.parkinglodging (주차 정보)
        private String roomcount;           // HotelDetail.roomcount (호텔 총 객실 수)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AreaDto {
        private String areaCode;            // HotelInfo.areaCode
        private String nearbyAttractions;   // 프론트엔드용
        private String transportation;     // 프론트엔드용
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageDto {
        private Long id;                    // HotelImage.id (타임스탬프 ID 지원을 위해 Long 사용)
        private String originUrl;           // HotelImage.originUrl
        private String smallUrl;            // HotelImage.smallUrl
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DiningDto {
        private Integer diningIdx;         // Dining.diningIdx
        private String name;                // Dining.name
        private String operatingHours;      // 프론트엔드용 (Dining.openTime + closeTime 변환)
        private String description;         // Dining.description
        private String content;             // Dining.content (상세 정보)
        private Integer basePrice;         // Dining.basePrice
        private Integer totalSeats;         // Dining.totalSeats
        private Integer slotDuration;       // Dining.slotDuration (예약 시간 단위)
        private Integer maxGuestsPerSlot;   // Dining.maxGuestsPerSlot (시간대별 최대 인원)
        private Integer status;             // Dining.status (0: 활성, 1: 비활성)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomDto {
        private Integer roomIdx;           // Room.roomIdx (수정 시 기존 객실 식별용, null이면 신규)
        private String name;                // Room.name
        private Integer capacity;           // Room.capacity
        private Integer basePrice;         // Room.basePrice
        private Boolean refundable;        // Room.refundable
        private Boolean breakfastIncluded; // Room.breakfastIncluded
        private Boolean smoking;           // Room.smoking
        private Integer roomCount;         // Room.roomCount (기본값 1)
        private Integer status;            // Room.status (0: 사용불가, 1: 사용가능)
        private String imageUrl;           // Room.imageUrl (객실 대표 이미지, 1장)
        private List<RoomImageDto> images;  // RoomImage 리스트 (객실 상세 이미지들)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomImageDto {
        private Integer roomImageIdx;      // RoomImage.roomImageIdx (고유키, null이면 신규 이미지)
        private String imageUrl;           // RoomImage.imageUrl
        private Integer imageOrder;        // RoomImage.imageOrder
    }
}

