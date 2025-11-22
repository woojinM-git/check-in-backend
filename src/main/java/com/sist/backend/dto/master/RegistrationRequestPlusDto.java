package com.sist.backend.dto.master;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        private String title;
        private String adress; // 하위 호환성 유지
        private String baseAddress; // 도로명 주소
        private String detailAddress; // 상세 주소
        private String phone;
        private String email;
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
            try {
                // HotelDraft에서 JSON을 파싱하여 HotelInfo 생성
                ObjectMapper objectMapper = new ObjectMapper();
                String formData = registrationRequest.getHotelDraft().getFormData();
                
                if (formData != null && !formData.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsedData = (Map<String, Object>) objectMapper.readValue(formData, Map.class);
                    
                    HotelInfo hotelInfo = new HotelInfo();
                    
                    // 기본 정보 파싱
                    if (parsedData.get("hotelInfo") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> hotelInfoMap = (Map<String, Object>) parsedData.get("hotelInfo");
                        hotelInfo.setTitle((String) hotelInfoMap.get("title"));
                        hotelInfo.setAdress((String) hotelInfoMap.get("adress")); // 하위 호환성 유지
                        hotelInfo.setBaseAddress((String) hotelInfoMap.get("baseAddress")); // 도로명 주소
                        hotelInfo.setDetailAddress((String) hotelInfoMap.get("detailAddress")); // 상세 주소
                        hotelInfo.setPhone((String) hotelInfoMap.get("phone"));
                        hotelInfo.setEmail((String) hotelInfoMap.get("email"));
                    }
                    
                    // 객실 개수 파싱
                    if (parsedData.get("rooms") instanceof java.util.List) {
                        java.util.List<?> rooms = (java.util.List<?>) parsedData.get("rooms");
                        hotelInfo.setRooms(rooms != null ? rooms.size() : 0);
                    } else {
                        hotelInfo.setRooms(0);
                    }
                    
                    dto.setHotelInfo(hotelInfo);
                }
            } catch (Exception e) {
                // 파싱 실패 시 빈 HotelInfo 객체 생성
                dto.setHotelInfo(new HotelInfo());
            }
        }
        return dto;
    }
}
