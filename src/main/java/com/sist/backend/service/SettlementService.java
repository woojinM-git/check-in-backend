package com.sist.backend.service;

import com.sist.backend.dto.master.SettlementDto;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.entity.HotelSettlement;
import com.sist.backend.repository.HotelSettlementRepository;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final HotelSettlementRepository settlementRepository;
    private final RoomPaymentRepository roomPaymentRepository;
    private final HotelInfoRepository hotelInfoRepository;

    private static final double DEFAULT_COMMISSION_RATE = 0.10; // 10%
    private static final double DEFAULT_WITHHOLDING_TAX_RATE = 0.033; // 3.3%

    /**
     * 특정 월의 정산 데이터 생성
     * @param year 연도
     * @param month 월 (1-12)
     */
    @Transactional
    public void createSettlementForMonth(int year, int month) {
        String settlementMonth = String.format("%04d-%02d", year, month);
        
        // 모든 활성 호텔 조회
        List<HotelInfo> hotels = hotelInfoRepository.findAll();
        
        for (HotelInfo hotel : hotels) {
            // 이미 정산 데이터가 있는지 확인
            if (settlementRepository.findByContentIdAndSettlementMonth(
                    hotel.getContentId(), settlementMonth).isPresent()) {
                continue; // 이미 정산 데이터가 있으면 스킵
            }

            // 해당 호텔의 월별 총 수익 계산
            Long totalRevenue = roomPaymentRepository.findTotalRevenueByContentIdAndMonth(
                hotel.getContentId(), year, month);

            if (totalRevenue == null || totalRevenue == 0) {
                continue; // 수익이 없으면 정산 데이터 생성하지 않음
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
                .contentId(hotel.getContentId())
                .settlementMonth(settlementMonth)
                .totalRevenue(totalRevenue)
                .commissionAmount(commissionAmount)
                .withholdingTaxAmount(withholdingTaxAmount)
                .finalAmount(finalAmount)
                .build();

            settlementRepository.save(settlement);
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

