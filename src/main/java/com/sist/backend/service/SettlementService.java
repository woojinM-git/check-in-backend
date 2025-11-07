package com.sist.backend.service;

import com.sist.backend.dto.master.SettlementDto;
import com.sist.backend.entity.HotelSettlement;
import com.sist.backend.repository.HotelSettlementRepository;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final HotelSettlementRepository settlementRepository;
    private final RoomPaymentRepository roomPaymentRepository;
    private final HotelInfoRepository hotelInfoRepository;

    private static final double DEFAULT_COMMISSION_RATE = 0.10; // 10%
    private static final double DEFAULT_WITHHOLDING_TAX_RATE = 0.033; // 3.3%

    /**
     * 특정 월의 정산 데이터 생성 (결제 기준 집계)
     * 결제가 발생한 호텔만 처리하여 성능 최적화
     * 
     * @param year 연도
     * @param month 월 (1-12)
     */
    public void createSettlementForMonth(int year, int month) {
        String settlementMonth = String.format("%04d-%02d", year, month);
        
        log.info("정산 데이터 생성 시작: {}년 {}월", year, month);
        
        // 1. 한 번의 쿼리로 결제가 발생한 모든 호텔의 수익 집계
        List<Object[]> hotelRevenues = roomPaymentRepository.findMonthlyRevenueByHotel(year, month);
        
        log.info("결제 발생 호텔 수: {}개", hotelRevenues.size());
        
        if (hotelRevenues.isEmpty()) {
            log.info("정산할 결제 내역이 없습니다.");
            return;
        }
        
        // 2. 배치 단위로 정산 데이터 생성 (트랜잭션 분리)
        int batchSize = 50;
        List<HotelRevenueData> batch = new ArrayList<>();
        
        for (Object[] row : hotelRevenues) {
            String contentId = (String) row[0];
            Long totalRevenue = ((Number) row[1]).longValue();
            
            batch.add(new HotelRevenueData(contentId, totalRevenue));
            
            // 배치 크기에 도달하면 처리
            if (batch.size() >= batchSize) {
                processSettlementBatch(batch, settlementMonth);
                batch.clear();
            }
        }
        
        // 남은 데이터 처리
        if (!batch.isEmpty()) {
            processSettlementBatch(batch, settlementMonth);
        }
        
        log.info("정산 데이터 생성 완료: {}년 {}월", year, month);
    }

    /**
     * 배치 단위 정산 처리 (트랜잭션 분리)
     */
    @Transactional
    public void processSettlementBatch(List<HotelRevenueData> hotelRevenues, String settlementMonth) {
        for (HotelRevenueData hotelData : hotelRevenues) {
            String contentId = hotelData.contentId;
            Long totalRevenue = hotelData.totalRevenue;
            
            // 이미 정산 데이터가 있는지 확인
            if (settlementRepository.findByContentIdAndSettlementMonth(
                    contentId, settlementMonth).isPresent()) {
                log.debug("이미 정산 데이터 존재: contentId={}, month={}", contentId, settlementMonth);
                continue;
            }
            
            // 수익이 없으면 스킵
            if (totalRevenue == null || totalRevenue == 0) {
                continue;
            }
            
            // 수수료 계산 (10%)
            Long commissionAmount = Math.round(totalRevenue * DEFAULT_COMMISSION_RATE);
            
            // 정산 대상 금액 (수수료 제외)
            Long settlementAmount = totalRevenue - commissionAmount;
            
            // 원천징수 계산 (3.3%)
            Long withholdingTaxAmount = Math.round(settlementAmount * DEFAULT_WITHHOLDING_TAX_RATE);
            
            // 최종 지급 금액 (원천징수 제외)
            Long finalAmount = settlementAmount - withholdingTaxAmount;
            
            // 정산 데이터 생성
            HotelSettlement settlement = HotelSettlement.builder()
                .contentId(contentId)
                .settlementMonth(settlementMonth)
                .totalRevenue(totalRevenue)
                .commissionAmount(commissionAmount)
                .withholdingTaxAmount(withholdingTaxAmount)
                .finalAmount(finalAmount)
                .build();
            
            settlementRepository.save(settlement);
            log.debug("정산 데이터 생성 완료: contentId={}, totalRevenue={}, finalAmount={}", 
                contentId, totalRevenue, finalAmount);
        }
    }

    /**
     * 호텔별 수익 데이터를 담는 내부 클래스
     */
    private static class HotelRevenueData {
        String contentId;
        Long totalRevenue;
        
        HotelRevenueData(String contentId, Long totalRevenue) {
            this.contentId = contentId;
            this.totalRevenue = totalRevenue;
        }
    }

    /**
     * 직전 달의 정산 데이터 생성 (일반적으로 사용)
     */
    @Transactional
    public void createSettlementForPreviousMonth() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        createSettlementForMonth(previousMonth.getYear(), previousMonth.getMonthValue());
    }

    /**
     * 정산 목록 조회 (페이징)
     */
    public Page<SettlementDto> findAllSettlements(Pageable pageable) {
        Page<HotelSettlement> settlements = settlementRepository.findAllOrderByMonthDesc(pageable);
        return settlements.map(settlement -> SettlementDto.fromEntity(settlement, hotelInfoRepository));
    }

    /**
     * 특정 호텔의 정산 목록 조회
     */
    public List<HotelSettlement> findByContentId(String contentId) {
        return settlementRepository.findByContentId(contentId);
    }

    /**
     * 특정 월의 정산 목록 조회
     */
    public List<HotelSettlement> findBySettlementMonth(String settlementMonth) {
        return settlementRepository.findBySettlementMonth(settlementMonth);
    }
}

