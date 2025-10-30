package com.sist.backend.dto.admin;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDto {
    private Today today;
    private List<RoomReservationDto> recentReservations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Today {
        private Integer checkinCount;
        private Integer checkoutCount;
        private Integer reservationCount;
        private Long thisMonthSales;
    }
}


