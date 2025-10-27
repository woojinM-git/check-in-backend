package com.sist.backend.dto.master;

import java.time.LocalDateTime;

import com.sist.backend.entity.RegistrationRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestPlusDto {
    // RegistrationRequest 기본 정보
    private Integer registrationIdx;
    private Integer adminIdx;
    private Integer draftIdx; // contentid 대신 draftIdx 사용
    private LocalDateTime regiDate;
    private Integer status;

    /* 어드민 정보 객체 */
    private Admin admin;
    /* 호텔 정보 객체 (HotelDraft JSON에서 파싱) */
    private HotelInfo hotelInfo;

    // 중첩 클래스들
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Admin {
        private Integer adminIdx;
        private String id;
        private String name;
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotelInfo {
        private String contentId;
        private String title;
        private String adress;
        private Integer rooms;
    }

    public static RegistrationRequestPlusDto fromEntity(RegistrationRequest registrationRequest) {
        RegistrationRequestPlusDto dto = new RegistrationRequestPlusDto();
        dto.setRegistrationIdx(registrationRequest.getRegistrationIdx());
        dto.setAdminIdx(registrationRequest.getAdminIdx());
        dto.setDraftIdx(registrationRequest.getDraftIdx()); // contentid 대신 draftIdx
        dto.setRegiDate(registrationRequest.getRegiDate());
        dto.setStatus(registrationRequest.getStatus());

        if(registrationRequest.getAdmin() != null) {
            Admin admin = new Admin();
            admin.setAdminIdx(registrationRequest.getAdmin().getAdminIdx());
            admin.setId(registrationRequest.getAdmin().getId());
            admin.setName(registrationRequest.getAdmin().getName());
            admin.setPhone(registrationRequest.getAdmin().getPhone());
            dto.setAdmin(admin);
        }

        if(registrationRequest.getHotelDraft() != null) {
            // HotelDraft에서 JSON을 파싱하여 HotelInfo 생성
            // 이 부분은 실제 구현 시 JSON 파싱 로직 추가 필요
            HotelInfo hotelInfo = new HotelInfo();
            // hotelInfo.setContentId(parsedData.getContentId());
            // hotelInfo.setTitle(parsedData.getTitle());
            // hotelInfo.setAdress(parsedData.getAdress());
            // hotelInfo.setRooms(parsedData.getRooms().size());
            dto.setHotelInfo(hotelInfo);
        }
        return dto;
    }
}
