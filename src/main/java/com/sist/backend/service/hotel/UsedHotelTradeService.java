package com.sist.backend.service.hotel;

import com.sist.backend.entity.UsedTrade;
import com.sist.backend.entity.UsedItem;
import com.sist.backend.entity.UsedPay;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.entity.Customer;
import com.sist.backend.repository.UsedTradeRepository;
import com.sist.backend.repository.UsedItemRepository;
import com.sist.backend.repository.UsedPayRepository;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.repository.CustomerRepository;
import com.sist.backend.service.UsedPaymentLockService;
import com.sist.backend.dto.UsedPaymentLockDto;
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
    private final CustomerRepository customerRepository;
    private final UsedPaymentLockService paymentLockService;

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
     * 결제 내역 생성 (Redis 분산 락 + DB 중복 체크 적용)
     * @param usedTradeIdx 거래 ID
     * @param paymentData 결제 데이터
     * @return 생성된 결제 내역
     */
    @Transactional
    public UsedPay createPayment(Integer usedTradeIdx, Map<String, Object> paymentData) {
        String paymentKey = (String) paymentData.get("paymentKey");
        String orderId = (String) paymentData.get("orderId");
        String lockKey = null;
        
        try {
            // 1단계: Redis 분산 락 획득 (DB 중복 체크 포함)
            // - paymentKey로 중복 결제 확인
            // - usedTradeIdx로 이미 결제된 거래 확인
            // - 거래 상태 확인
            // (buyerIdx는 락 획득 후에 조회하므로 임시로 null 전달)
            UsedPaymentLockDto lockResult = paymentLockService.createLock(
                usedTradeIdx, 
                paymentKey, 
                orderId, 
                null // buyerIdx는 나중에 조회
            );
            
            if (!lockResult.getSuccess()) {
                log.warn("결제 락 획득 실패: {} - {}", usedTradeIdx, lockResult.getMessage());
                throw new RuntimeException(lockResult.getMessage());
            }
            
            lockKey = lockResult.getLockKey();
            log.info("결제 락 획득 성공: lockKey={}, usedTradeIdx={}", lockKey, usedTradeIdx);
            
            // 2단계: DB Pessimistic Lock으로 거래 조회 (트랜잭션 내에서)
            // SELECT FOR UPDATE로 row를 잠가서 다른 트랜잭션의 수정을 방지
            // 거래가 삭제되었을 수 있으므로 Optional로 처리
            Optional<UsedTrade> tradeOpt = usedTradeRepository.findByIdForUpdate(usedTradeIdx);
            if (tradeOpt.isEmpty()) {
                log.warn("거래를 찾을 수 없습니다 (삭제되었을 수 있음): usedTradeIdx={}", usedTradeIdx);
                throw new RuntimeException("거래를 찾을 수 없습니다. 거래가 취소되었거나 삭제되었을 수 있습니다.");
            }
            UsedTrade trade = tradeOpt.get();
            
            // 3단계: 트랜잭션 내에서 다시 한 번 DB 중복 체크 (이중 방어)
            // 락 획득 후 트랜잭션 내에서 한 번 더 확인
            Optional<UsedPay> existingPayment = usedPayRepository.findByPaymentKey(paymentKey);
            if (existingPayment.isPresent()) {
                log.warn("이미 처리된 결제 (락 획득 후 DB 재확인): paymentKey={}, usedPayIdx={}", 
                        paymentKey, existingPayment.get().getUsedPayIdx());
                throw new RuntimeException("이미 처리된 결제입니다.");
            }
            
            // 거래 상태 재확인 (Pessimistic Lock으로 조회한 최신 데이터)
            if (trade.getStstus() != 0) {
                log.warn("이미 처리된 거래 (락 획득 후 재확인): usedTradeIdx={}, status={}", 
                        usedTradeIdx, trade.getStstus());
                throw new RuntimeException("이미 처리된 거래입니다.");
            }
            
            // 4단계: 결제 내역 생성
            UsedPay usedPay = new UsedPay();
            usedPay.setUsedTradeIdx(usedTradeIdx);
            usedPay.setPaymentKey(paymentKey);
            usedPay.setOrderId(orderId);
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
            
            // 5단계: 거래 확정
            trade.setStstus(1); // 거래완료 상태
            usedTradeRepository.save(trade);
            
            // 6단계: UsedItem 상태 업데이트
            Optional<UsedItem> usedItem = usedItemRepository.findById(trade.getUserItemIdx());
            if (usedItem.isPresent()) {
                UsedItem item = usedItem.get();
                item.setStatus(2); // 거래완료 상태
                usedItemRepository.save(item);
                
                // 7단계: RoomReservation의 customerIdx를 구매자(buyerIdx)로 변경
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
            
            // 8단계: 판매자 캐시 적립 (결제 총액 기준)
            try {
                Integer sellerIdx = trade.getSellerIdx();
                Optional<Customer> sellerOpt = customerRepository.findById(sellerIdx);
                if (sellerOpt.isPresent()) {
                    Customer seller = sellerOpt.get();
                    int currentCash = seller.getCash() != null ? seller.getCash() : 0;
                    int credit = usedPay.getTotalAmount() != null ? usedPay.getTotalAmount() : 0;
                    seller.setCash(currentCash + credit);
                    customerRepository.save(seller);
                    log.info("판매자 캐시 적립: sellerIdx={}, +{} => {}", sellerIdx, credit, seller.getCash());
                } else {
                    log.warn("판매자 정보를 찾을 수 없습니다: sellerIdx={}", sellerIdx);
                }
            } catch (Exception e) {
                log.error("판매자 캐시 적립 실패: {}", e.getMessage());
                // 결제/거래 확정 자체는 유지. 필요 시 정책에 따라 롤백 고려
            }
            
            log.info("결제 내역 생성 및 거래 확정 완료: {} (거래: {})", savedPayment.getUsedPayIdx(), usedTradeIdx);
            return savedPayment;
            
        } catch (RuntimeException e) {
            // 비즈니스 예외는 그대로 전파
            log.error("결제 내역 생성 실패: {} - {}", usedTradeIdx, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("결제 내역 생성 중 예외 발생: {} - {}", usedTradeIdx, e.getMessage(), e);
            throw new RuntimeException("결제 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        } finally {
            // 8단계: 락 해제 (성공/실패 관계없이 항상 해제)
            if (lockKey != null) {
                try {
                    UsedPaymentLockDto releaseResult = paymentLockService.releaseLock(lockKey, null);
                    if (releaseResult.getSuccess()) {
                        log.info("결제 락 해제 성공: lockKey={}", lockKey);
                    } else {
                        log.warn("결제 락 해제 실패: lockKey={}, message={}", lockKey, releaseResult.getMessage());
                    }
                } catch (Exception e) {
                    log.error("결제 락 해제 중 예외 발생: lockKey={}, error={}", lockKey, e.getMessage(), e);
                    // 락 해제 실패는 로그만 남기고 예외를 던지지 않음 (TTL로 자동 만료됨)
                }
            }
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
