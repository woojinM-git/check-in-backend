package com.sist.backend.dto.master;

import com.sist.backend.entity.HotelSettlement;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDto {
    private Integer settlementIdx;
    private String contentId;
    private String hotelTitle;
    private String settlementMonth;
    private Long totalRevenue;
    private Long commissionAmount;
    private Long withholdingTaxAmount;
    private Long finalAmount;

    public static SettlementDto fromEntity(HotelSettlement settlement, HotelInfoRepository hotelInfoRepository) {
        SettlementDto dto = new SettlementDto();
        dto.setSettlementIdx(settlement.getSettlementIdx());
        dto.setContentId(settlement.getContentId());
        
        // HotelInfo 조회
        String hotelTitle = hotelInfoRepository.findById(settlement.getContentId())
            .map(hotel -> hotel.getTitle())
            .orElse(settlement.getContentId());
        dto.setHotelTitle(hotelTitle);
        
        dto.setSettlementMonth(settlement.getSettlementMonth());
        dto.setTotalRevenue(settlement.getTotalRevenue());
        dto.setCommissionAmount(settlement.getCommissionAmount());
        dto.setWithholdingTaxAmount(settlement.getWithholdingTaxAmount());
        dto.setFinalAmount(settlement.getFinalAmount());
        return dto;
    }
}


