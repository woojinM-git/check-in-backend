package com.sist.backend.dto.master;

import java.time.LocalDateTime;

import com.sist.backend.entity.HotelInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelInfoDto {
    // HotelInfo 기본 정보
    private String contentId;
    private Integer adminIdx;
    private String title;
    private String adress;
    private String tel;
    private String hotelCategoryCode; // 카테고리 코드
    private String areaCode; // 지역 코드
    private String imageUrl; // 이미지 URL
    private Integer status; // 상태
    private LocalDateTime createdAt; // 생성일
    private LocalDateTime updatedAt; // 수정일

    /* 호텔 상세 정보 객체 */
    private HotelDetail hotelDetail;
    /* 어드민 정보 객체 */
    private Admin admin;

    // 중첩 클래스들
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotelDetail {
        private String roomcount;
        private String foodplace;
        private String parkinglodging;
        private String reservationlodging;
        private String scalelodging;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Admin {
        private String adminName;
        private String adminEmail;
        private String adminPhone;
    }

    public static HotelInfoDto hotelInfoDto(HotelInfo hotelInfo) {
        HotelInfoDto dto = new HotelInfoDto();
        
        // HotelInfo 기본 정보
        dto.setContentId(hotelInfo.getContentId());
        dto.setAdminIdx(hotelInfo.getAdminIdx());
        dto.setTitle(hotelInfo.getTitle());
        dto.setAdress(hotelInfo.getAdress());
        dto.setTel(hotelInfo.getTel());
        dto.setHotelCategoryCode(hotelInfo.getHotelCategoryCode());
        dto.setAreaCode(hotelInfo.getAreaCode());
        dto.setImageUrl(hotelInfo.getImageUrl());
        dto.setStatus(hotelInfo.getStatus());
        
        // HotelDetail 정보 (조인된 데이터)
        if (hotelInfo.getHotelDetail() != null) {
            HotelDetail hotelDetail = new HotelDetail();
            hotelDetail.setRoomcount(hotelInfo.getHotelDetail().getRoomcount());
            hotelDetail.setFoodplace(hotelInfo.getHotelDetail().getFoodplace());
            hotelDetail.setParkinglodging(hotelInfo.getHotelDetail().getParkinglodging());
            hotelDetail.setReservationlodging(hotelInfo.getHotelDetail().getReservationlodging());
            hotelDetail.setScalelodging(hotelInfo.getHotelDetail().getScalelodging());
            dto.setHotelDetail(hotelDetail);
        }
        
        // Admin 정보 (조인된 데이터)
        if (hotelInfo.getAdmin() != null) {
            Admin admin = new Admin();
            admin.setAdminName(hotelInfo.getAdmin().getName());
            admin.setAdminEmail(hotelInfo.getAdmin().getId());
            admin.setAdminPhone(hotelInfo.getAdmin().getPhone());
            dto.setAdmin(admin);
        }

        return dto;
    }
}