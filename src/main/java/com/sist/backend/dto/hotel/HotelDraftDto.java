package com.sist.backend.dto.hotel;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelDraftDto {
    
    private Integer draftIdx;
    private Integer adminIdx;
    private String formData;
    private String lastTab;
    private Integer progress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static HotelDraftDto fromEntity(com.sist.backend.entity.HotelDraft hotelDraft) {
        HotelDraftDto dto = new HotelDraftDto();
        dto.setDraftIdx(hotelDraft.getDraftIdx());
        dto.setAdminIdx(hotelDraft.getAdminIdx());
        dto.setFormData(hotelDraft.getFormData());
        dto.setLastTab(hotelDraft.getLastTab());
        dto.setProgress(hotelDraft.getProgress());
        dto.setCreatedAt(hotelDraft.getCreatedAt());
        dto.setUpdatedAt(hotelDraft.getUpdatedAt());
        return dto;
    }
}
