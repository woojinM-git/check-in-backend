package com.sist.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.service.AdminManagementService;
import com.sist.backend.service.RoomPaymentService;
import com.sist.backend.service.RoomReservationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminManagementController {
    
    private final AdminManagementService amService;
    private final RoomReservationService roomReservationService;
    private final RoomPaymentService roomPaymentService;

    /* 호텔 관리자 대시보드 화면 */
    /* 오늘 체크인, 오늘 체크아웃(roomReservation), 예약 대기, 이번달 매출 */
    /* 최근 예약 현황 */
    @RequestMapping("/dashboard")
    @Operation(summary = "대시보드 관리자", description = "관리자 대시보드 화면")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public Map<String, Object> dashboard(){
        Map<String, Object> map = new HashMap<>();
        // 오늘 체크인한 사람의 수
        Integer todayCheckinCount = roomReservationService.getTodayCheckinCount();
        map.put("todayCheckinCount", todayCheckinCount != null ? todayCheckinCount : 0);
        // 오늘 체크아웃한 사람의 수
        Integer todayCheckoutCount = roomReservationService.getTodayCheckoutCount();
        map.put("todayCheckoutCount", todayCheckoutCount != null ? todayCheckoutCount : 0);
        // 예약 확정 사람 수
        Integer reservationCount = roomReservationService.findByStatus();
        map.put("reservationCount", reservationCount != null ? reservationCount : 0);
        // 이번달 매출
        Long thisMonthSales = roomPaymentService.findByPrice();
        map.put("thisMonthSales", thisMonthSales != null ? thisMonthSales : 0);
        return map;
    }
}
