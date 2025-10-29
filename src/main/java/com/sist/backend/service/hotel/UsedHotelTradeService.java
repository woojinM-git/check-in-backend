package com.sist.backend.service.hotel;

import com.sist.backend.entity.UsedTrade;
import com.sist.backend.entity.UsedItem;
import com.sist.backend.entity.UsedPay;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.UsedTradeRepository;
import com.sist.backend.repository.UsedItemRepository;
import com.sist.backend.repository.UsedPayRepository;
import com.sist.backend.repository.RoomReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 중고 호텔 거래 동시성 제어 서비스
 * DB 트랜잭션 기반으로 동시성 문제 해결
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsedHotelTradeService {

    private final UsedTradeRepository usedTradeRepository;
    private final UsedItemRepository usedItemRepository;
    private final UsedPayRepository usedPayRepository;
    private final RoomReservationRepository roomReservationRepository;

    /**
     * 중고 아이템 거래 가능 여부 확인
     * @param usedItemIdx 중고 아이템 ID
     * @return 거래 가능 여부
     */
    @Transactional(readOnly = true)
    public boolean isUsedItemAvailable(Integer usedItemIdx) {
        // 1. UsedItem 상태 확인
        Optional<UsedItem> usedItem = usedItemRepository.findById(usedItemIdx);
        if (usedItem.isEmpty() || usedItem.get().getStatus() != 0) {
            log.info("중고 아이템이 거래 불가능 상태: {}", usedItemIdx);
            return false;
        }

        // 2. 활성 거래 확인
        boolean isAvailable = usedTradeRepository.isUsedItemAvailable(usedItemIdx);
        
        log.info("중고 아이템 거래 가능성 체크: {} = {}", usedItemIdx, isAvailable);
        return isAvailable;
    }

    /**
     * 중고 호텔 거래 생성 (동시성 제어)
     * @param usedItemIdx 중고 아이템 ID
     * @param buyerIdx 구매자 ID
     * @param sellerIdx 판매자 ID
     * @param price 거래 가격
     * @param reservIdx 예약 ID
     * @return 생성된 거래 또는 null (실패 시)
     */
    @Transactional
    public UsedTrade createUsedTrade(Integer usedItemIdx, Integer buyerIdx, 
                                   Integer sellerIdx, Integer price, Integer reservIdx) {
        try {
            // 1. 다시 한번 거래 가능성 체크 (트랜잭션 내에서)
            if (!isUsedItemAvailable(usedItemIdx)) {
                log.warn("중고 아이템이 이미 거래됨: {}", usedItemIdx);
                return null;
            }

            // 2. 거래 생성
            UsedTrade usedTrade = new UsedTrade();
            usedTrade.setUserItemIdx(usedItemIdx);
            usedTrade.setBuyerIdx(buyerIdx);
            usedTrade.setSellerIdx(sellerIdx);
            usedTrade.setPrice(price);
            usedTrade.setReservIdx(reservIdx);
            usedTrade.setStstus(0); // 거래중 상태
            // createdAt, updatedAt은 @PrePersist에서 자동 설정

            UsedTrade savedTrade = usedTradeRepository.save(usedTrade);
            
            // 3. UsedItem 상태 업데이트 (거래 중으로 변경)
            Optional<UsedItem> usedItem = usedItemRepository.findById(usedItemIdx);
            if (usedItem.isPresent()) {
                // status: 0 = 판매중, 1 = 거래중, 2 = 거래완료(판매완료), 3 = 만료
                usedItem.get().setStatus(1); // 거래중 상태
                usedItemRepository.save(usedItem.get());
            }
            
            log.info("중고 호텔 거래 생성 성공: {} (구매자: {}, 판매자: {})", 
                    savedTrade.getUsedTradeIdx(), buyerIdx, sellerIdx);
            return savedTrade;
            
        } catch (Exception e) {
            log.error("중고 호텔 거래 생성 실패: {}", e.getMessage());
            throw e; // 트랜잭션 롤백
        }
    }

    /**
     * 거래 확정 (결제 완료 후)
     * @param usedTradeIdx 거래 ID
     * @return 확정된 거래
     */
    @Transactional
    public UsedTrade confirmTrade(Integer usedTradeIdx) {
        UsedTrade trade = usedTradeRepository.findById(usedTradeIdx)
            .orElseThrow(() -> new RuntimeException("거래를 찾을 수 없습니다: " + usedTradeIdx));
        
        if (trade.getStstus() != 0) {
            throw new RuntimeException("이미 처리된 거래입니다: " + usedTradeIdx);
        }
        
        trade.setStstus(1); // 거래완료 상태
        
        // UsedItem 상태도 업데이트
        Optional<UsedItem> usedItem = usedItemRepository.findById(trade.getUserItemIdx());
        if (usedItem.isPresent()) {
            // status: 0 = 판매중, 1 = 거래중, 2 = 거래완료(판매완료), 3 = 만료
            usedItem.get().setStatus(2); // 거래완료(판매완료) 상태
            usedItemRepository.save(usedItem.get());
        }
        
        UsedTrade confirmedTrade = usedTradeRepository.save(trade);
        log.info("중고 호텔 거래 확정: {}", usedTradeIdx);
        
        return confirmedTrade;
    }

    /**
     * 거래 삭제 (페이지 이탈 시) - 최적화된 버전
     * @param usedTradeIdx 거래 ID
     * @param deleteReason 삭제 사유
     */
    @Transactional
    public void deleteTrade(Integer usedTradeIdx, String deleteReason) {
        try {
            // 거래 존재 여부 확인
            Optional<UsedTrade> tradeOpt = usedTradeRepository.findById(usedTradeIdx);
            if (!tradeOpt.isPresent()) {
                log.warn("삭제할 거래가 존재하지 않습니다: {}", usedTradeIdx);
                return; // 이미 삭제된 경우 조용히 리턴
            }
            
            UsedTrade trade = tradeOpt.get();
            
            // 거래 상태가 대기 중(0)인 경우에만 삭제
            if (trade.getStstus() != 0) {
                log.warn("이미 처리된 거래는 삭제할 수 없습니다: {} (상태: {})", usedTradeIdx, trade.getStstus());
                return;
            }
            
            // UsedItem 상태 복원
            Optional<UsedItem> usedItem = usedItemRepository.findById(trade.getUserItemIdx());
            if (usedItem.isPresent()) {
                // status: 0 = 판매중, 1 = 거래중, 2 = 거래완료(판매완료), 3 = 만료
                usedItem.get().setStatus(0); // 판매중 상태로 복원
                usedItemRepository.save(usedItem.get());
                log.info("UsedItem 상태 복원: {} -> 판매중", trade.getUserItemIdx());
            }
            
            // 거래 삭제
            usedTradeRepository.delete(trade);
            log.info("중고 호텔 거래 삭제: {} (사유: {})", usedTradeIdx, deleteReason);
            
        } catch (Exception e) {
            log.error("거래 삭제 중 오류 발생: {} - {}", usedTradeIdx, e.getMessage());
            throw e;
        }
    }

    /**
     * 거래 취소
     * @param usedTradeIdx 거래 ID
     * @param cancelReason 취소 사유
     */
    @Transactional
    public void cancelTrade(Integer usedTradeIdx, String cancelReason) {
        UsedTrade trade = usedTradeRepository.findById(usedTradeIdx)
            .orElseThrow(() -> new RuntimeException("거래를 찾을 수 없습니다: " + usedTradeIdx));
        
        trade.setStstus(2); // 거래취소 상태
        
        // UsedItem 상태 복원
        Optional<UsedItem> usedItem = usedItemRepository.findById(trade.getUserItemIdx());
        if (usedItem.isPresent()) {
            // status: 0 = 판매중, 1 = 거래중, 2 = 거래완료(판매완료), 3 = 만료
            usedItem.get().setStatus(0); // 판매중 상태로 복원
            usedItemRepository.save(usedItem.get());
        }
        
        usedTradeRepository.save(trade);
        log.info("중고 호텔 거래 취소: {} (사유: {})", usedTradeIdx, cancelReason);
    }

    /**
     * 오래된 대기 거래 정리 (스케줄러에서 호출)
     * 1분 이상 대기 상태인 거래를 자동 취소 (테스트용)
     */
    @Transactional
    public void cleanupExpiredTrades() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(1); // 테스트용: 1분
        
        List<UsedTrade> expiredTrades = usedTradeRepository.findExpiredPendingTrades(cutoffTime);
        
        for (UsedTrade trade : expiredTrades) {
            cancelTrade(trade.getUsedTradeIdx(), "자동 취소 (1분 초과)");
        }
        
        if (!expiredTrades.isEmpty()) {
            log.info("만료된 거래 {}개 자동 취소 완료", expiredTrades.size());
        }
    }

    /**
     * 거래 상태 조회
     * @param usedTradeIdx 거래 ID
     * @return 거래 상태 (0: 거래중, 1: 거래완료, 2: 거래취소)
     */
    @Transactional(readOnly = true)
    public Integer getTradeStatus(Integer usedTradeIdx) {
        return usedTradeRepository.findById(usedTradeIdx)
            .map(UsedTrade::getStstus)
            .orElse(null);
    }

    /**
     * 구매자의 거래 목록 조회
     * @param buyerIdx 구매자 ID
     * @return 거래 목록
     */
    @Transactional(readOnly = true)
    public List<UsedTrade> getBuyerTrades(Integer buyerIdx) {
        return usedTradeRepository.findByBuyerIdx(buyerIdx);
    }

    /**
     * 판매자의 거래 목록 조회
     * @param sellerIdx 판매자 ID
     * @return 거래 목록
     */
    @Transactional(readOnly = true)
    public List<UsedTrade> getSellerTrades(Integer sellerIdx) {
        return usedTradeRepository.findBySellerIdx(sellerIdx);
    }

    /**
     * 결제 내역 생성
     * @param usedTradeIdx 거래 ID
     * @param paymentData 결제 데이터
     * @return 생성된 결제 내역
     */
    @Transactional
    public UsedPay createPayment(Integer usedTradeIdx, Map<String, Object> paymentData) {
        try {
            // 거래 존재 확인
            UsedTrade trade = usedTradeRepository.findById(usedTradeIdx)
                .orElseThrow(() -> new RuntimeException("거래를 찾을 수 없습니다: " + usedTradeIdx));

            // 결제 내역 생성
            UsedPay usedPay = new UsedPay();
            usedPay.setUsedTradeIdx(usedTradeIdx);
            usedPay.setPaymentKey((String) paymentData.get("paymentKey"));
            usedPay.setOrderId((String) paymentData.get("orderId"));
            usedPay.setTotalAmount((Integer) paymentData.get("totalAmount"));
            usedPay.setCashAmount((Integer) paymentData.get("cashAmount"));
            usedPay.setPointAmount((Integer) paymentData.get("pointAmount"));
            usedPay.setCardAmount((Integer) paymentData.get("cardAmount"));
            usedPay.setPaymentMethod((String) paymentData.get("paymentMethod"));
            usedPay.setStatus(1); // 결제 완료
            usedPay.setReceiptUrl((String) paymentData.get("receiptUrl"));
            usedPay.setQrUrl((String) paymentData.get("qrUrl"));
            usedPay.setApprovedAt(LocalDateTime.now());

            UsedPay savedPayment = usedPayRepository.save(usedPay);
            
            // 1. 거래 확정
            trade.setStstus(1); // 거래완료 상태
            usedTradeRepository.save(trade);
            
            // 2. UsedItem 상태 업데이트
            Optional<UsedItem> usedItem = usedItemRepository.findById(trade.getUserItemIdx());
            if (usedItem.isPresent()) {
                UsedItem item = usedItem.get();
                item.setStatus(2); // 거래완료 상태
                usedItemRepository.save(item);
                
                // 3. RoomReservation의 customerIdx를 구매자(buyerIdx)로 변경
                Integer reservIdx = item.getReservIdx();
                Optional<RoomReservation> reservationOpt = roomReservationRepository.findById(reservIdx);
                
                if (reservationOpt.isPresent()) {
                    RoomReservation reservation = reservationOpt.get();
                    Integer oldCustomerIdx = reservation.getCustomerIdx();
                    reservation.setCustomerIdx(trade.getBuyerIdx());
                    roomReservationRepository.save(reservation);
                    log.info("예약 고객 ID 변경: reservIdx={}, 기존 customerIdx={}, 새로운 customerIdx={}", 
                        reservIdx, oldCustomerIdx, trade.getBuyerIdx());
                } else {
                    log.warn("예약을 찾을 수 없습니다: reservIdx={}", reservIdx);
                }
            }
            
            log.info("결제 내역 생성 및 거래 확정 완료: {} (거래: {})", savedPayment.getUsedPayIdx(), usedTradeIdx);
            return savedPayment;
            
        } catch (Exception e) {
            log.error("결제 내역 생성 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 결제 내역 조회
     * @param usedTradeIdx 거래 ID
     * @return 결제 내역 목록
     */
    @Transactional(readOnly = true)
    public List<UsedPay> getPaymentsByTrade(Integer usedTradeIdx) {
        return usedPayRepository.findByUsedTradeIdxOrderByCreatedAtDesc(usedTradeIdx);
    }

    /**
     * 결제 내역 조회 (결제 키로)
     * @param paymentKey 토스페이먼츠 결제 키
     * @return 결제 내역 또는 null
     */
    @Transactional(readOnly = true)
    public Optional<UsedPay> getPaymentByKey(String paymentKey) {
        return usedPayRepository.findByPaymentKey(paymentKey);
    }
}
