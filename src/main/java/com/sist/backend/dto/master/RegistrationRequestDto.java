package com.sist.backend.dto.master;

import java.time.LocalDateTime;

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

    /* 어드민 정보 객체 */
    private Admin admin;
    /* 호텔 정보 객체 */
    private HotelInfo hotelInfo;

    // 중첩 클래스들
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Admin {
        private Integer adminIdx;
        private String adminName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotelInfo {
        private String contentId;
        private String title;
        private String adress;
        private Integer rooms;
        private Integer count;
    }

    public static RegistrationRequestDto fromEntity(RegistrationRequest registrationRequest) {
        RegistrationRequestDto dto = new RegistrationRequestDto();
        dto.setRegistrationIdx(registrationRequest.getRegistrationIdx());
        dto.setAdminIdx(registrationRequest.getAdminIdx());
        dto.setContentid(registrationRequest.getContentid());
        dto.setRegiDate(registrationRequest.getRegiDate());
        dto.setStatus(registrationRequest.getStatus());

        if(registrationRequest.getAdmin() != null) {
            Admin admin = new Admin();
            admin.setAdminIdx(registrationRequest.getAdmin().getAdminIdx());
            admin.setAdminName(registrationRequest.getAdmin().getName());
            dto.setAdmin(admin);
        }

        if(registrationRequest.getHotelInfo() != null) {
            HotelInfo hotelInfo = new HotelInfo();
            hotelInfo.setContentId(registrationRequest.getHotelInfo().getContentId());
            hotelInfo.setTitle(registrationRequest.getHotelInfo().getTitle());
            hotelInfo.setAdress(registrationRequest.getHotelInfo().getAdress());
            hotelInfo.setRooms(registrationRequest.getHotelInfo().getRooms().size());
            dto.setHotelInfo(hotelInfo);
        }
        return dto;
    }
}
