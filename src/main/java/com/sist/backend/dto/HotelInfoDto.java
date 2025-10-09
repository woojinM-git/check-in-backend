package com.sist.backend.dto;

import com.sist.backend.entity.HotelInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelInfoDto {
    
    private String contentId;
    private String title;
    private String adress;
    private String tel;
    private String imageUrl;
    
    // Category 정보
    private String hotelCategoryCode;
    private String categoryName;
    
    // Area 정보
    private String areaCode;
    private String areaName;
    
    /**
     * Entity를 DTO로 변환
     */
    public static HotelInfoDto fromEntity(HotelInfo hotelInfo) {
        HotelInfoDto dto = new HotelInfoDto();
        dto.setContentId(hotelInfo.getContentId());
        dto.setTitle(hotelInfo.getTitle());
        dto.setAdress(hotelInfo.getAdress());
        dto.setTel(hotelInfo.getTel());
        dto.setImageUrl(hotelInfo.getImageUrl());
        
        // Category 정보
        if (hotelInfo.getCategory() != null) {
            dto.setHotelCategoryCode(hotelInfo.getCategory().getHotelCategoryCode());
            dto.setCategoryName(hotelInfo.getCategory().getCategoryName());
        }
        
        // Area 정보
        if (hotelInfo.getArea() != null) {
            dto.setAreaCode(hotelInfo.getArea().getAreaCode());
            dto.setAreaName(hotelInfo.getArea().getAreaName());
        }
        
        return dto;
    }
}

