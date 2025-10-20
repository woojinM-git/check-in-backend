package com.sist.backend.dto.master;

import java.time.LocalDateTime;

import com.sist.backend.entity.Admin;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.entity.RegistrationRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestDto {
    // RegistrationRequest 기본 정보
    private Integer registrationIdx;
    private Integer adminIdx;
    private String contentid;
    private LocalDateTime regiDate;
    private Integer status;
    private LocalDateTime approvDate;

    /* 어드민 정보 객체 */
    private Admin admin;
    /* 호텔 정보 객체 */
    private HotelInfo hotelInfo;

    // 중첩 클래스들
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Admin {
        private String adminName;
        private String adminEmail;
        private String adminPhone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotelInfo {
        private String contentId;
        private String title;
        private String adress;
        private String tel;
        private String hotelCategoryCode;
        private String areaCode;
        private Integer rooms;
    }

    public static RegistrationRequestDto fromEntity(RegistrationRequest registrationRequest) {
        RegistrationRequestDto dto = new RegistrationRequestDto();
        dto.setRegistrationIdx(registrationRequest.getRegistrationIdx());
        dto.setAdminIdx(registrationRequest.getAdminIdx());
        dto.setContentid(registrationRequest.getContentid());
        dto.setRegiDate(registrationRequest.getRegiDate());
        dto.setStatus(registrationRequest.getStatus());
        dto.setApprovDate(registrationRequest.getApprovDate());

        if(registrationRequest.getAdmin() != null) {
            Admin admin = new Admin();
            admin.setAdminName(registrationRequest.getAdmin().getName());
            admin.setAdminEmail(registrationRequest.getAdmin().getId());
            admin.setAdminPhone(registrationRequest.getAdmin().getPhone());
            dto.setAdmin(admin);
        }
        
        if(registrationRequest.getHotelInfo() != null) {
            HotelInfo hotelInfo = new HotelInfo();
            hotelInfo.setContentId(registrationRequest.getHotelInfo().getContentId());
            hotelInfo.setTitle(registrationRequest.getHotelInfo().getTitle());
            hotelInfo.setAdress(registrationRequest.getHotelInfo().getAdress());
            hotelInfo.setTel(registrationRequest.getHotelInfo().getTel());
            hotelInfo.setHotelCategoryCode(registrationRequest.getHotelInfo().getHotelCategoryCode());
            hotelInfo.setAreaCode(registrationRequest.getHotelInfo().getAreaCode());
            dto.setHotelInfo(hotelInfo);
        }
        return dto;
    }
}
