package com.sist.backend.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sist.backend.dto.admin.RevenueSummaryDto;
import com.sist.backend.dto.admin.RevenueSummaryDto.MonthRevenueDto;
import com.sist.backend.dto.admin.RevenueSummaryDto.RoomRevenueDto;
import com.sist.backend.dto.admin.DailyRevenueDto;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.entity.Room;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final RoomPaymentRepository roomPaymentRepository;
    private final com.sist.backend.service.RoomService roomService;

    public RevenueSummaryDto getRevenueSummary(String contentId) {
        List<RoomPayment> payments = roomPaymentRepository.findAllByContentIdWithReservations(contentId);
        List<Room> allRooms = roomService.findByContentIdAdmin(contentId);

        LocalDate today = LocalDate.now();

        long todayRevenue = 0L;
        int todayCount = 0;

        Map<YearMonth, Long> monthToRevenue = new HashMap<>();
        Map<String, Long> roomToRevenue = new HashMap<>();
        Map<String, Integer> roomToCount = new HashMap<>();

        for (RoomPayment rp : payments) {
            long price = rp.getPrice() != null ? rp.getPrice().longValue() : 0L;

            // 집계 기준 날짜: RoomPayment.approvedAt (LocalDateTime 가정)
            LocalDate refDate = null;
            if (rp.getApprovedAt() != null) {
                refDate = rp.getApprovedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            }

            if (refDate != null && refDate.isEqual(today)) {
                todayRevenue += price;
                todayCount += 1;
            }

            if (refDate != null) {
                YearMonth ym = YearMonth.from(refDate);
                monthToRevenue.merge(ym, price, Long::sum);
            }

            String roomName = null;
            // 객실명은 첫 예약의 Room.name 기준 (결제-객실 연결이 예약을 통해 있는 경우)
            RoomReservation base = null;
            if (rp.getRoomReservations() != null && !rp.getRoomReservations().isEmpty()) {
                base = rp.getRoomReservations().get(0);
                if (base.getRoom() != null) {
                    roomName = base.getRoom().getName();
                }
            }
            if (roomName != null) {
                roomToRevenue.merge(roomName, price, Long::sum);
                roomToCount.merge(roomName, 1, Integer::sum);
            }
        }

        List<MonthRevenueDto> monthly = new ArrayList<>();
        YearMonth start = YearMonth.now().minusMonths(11);
        for (int i = 0; i < 12; i++) {
            YearMonth ym = start.plusMonths(i);
            Long revenue = monthToRevenue.getOrDefault(ym, 0L);
            monthly.add(MonthRevenueDto.builder()
                .month(ym.atDay(1))
                .revenue(revenue)
                .build());
        }

        List<RoomRevenueDto> byRoom = new ArrayList<>();
        // 결제가 없어도 모든 객실 포함
        for (Room room : allRooms) {
            String rname = room.getName();
            Long rev = roomToRevenue.getOrDefault(rname, 0L);
            Integer cnt = roomToCount.getOrDefault(rname, 0);
            byRoom.add(RoomRevenueDto.builder()
                .roomName(rname)
                .revenue(rev)
                .count(cnt)
                .build());
        }

        return RevenueSummaryDto.builder()
            .todayRevenue(todayRevenue)
            .todayPayments(todayCount)
            .monthlyRevenue(monthly)
            .revenueByRoom(byRoom)
            .build();
    }

    public List<DailyRevenueDto> getDailyRevenue(String contentId, LocalDate start, LocalDate end) {
        List<RoomPayment> payments = roomPaymentRepository.findAllByContentIdWithReservations(contentId);
        Map<LocalDate, DailyRevenueDto> dayToAgg = new HashMap<>();

        // 초기 구간 생성 (누락일 0 채우기)
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            dayToAgg.put(cur, DailyRevenueDto.builder().day(cur).revenue(0L).payments(0).build());
            cur = cur.plusDays(1);
        }

        for (RoomPayment rp : payments) {
            LocalDate refDate = null;
            if (rp.getApprovedAt() != null) {
                refDate = rp.getApprovedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            if (refDate == null) continue;
            if (refDate.isBefore(start) || refDate.isAfter(end)) continue;

            long price = rp.getPrice() != null ? rp.getPrice().longValue() : 0L;
            DailyRevenueDto agg = dayToAgg.get(refDate);
            if (agg != null) {
                agg.setRevenue(agg.getRevenue() + price);
                agg.setPayments(agg.getPayments() + 1);
            }
        }

        List<DailyRevenueDto> rows = new ArrayList<>(dayToAgg.values());
        rows.sort((a, b) -> a.getDay().compareTo(b.getDay()));
        return rows;
    }
}


