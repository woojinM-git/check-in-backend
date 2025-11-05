package com.sist.backend.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sist.backend.dto.UsedPaymentLockDto;
import com.sist.backend.entity.UsedPay;
import com.sist.backend.entity.UsedTrade;
import com.sist.backend.repository.UsedPayRepository;
import com.sist.backend.repository.UsedTradeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 중고거래 결제 락 관리 서비스 (Redis 기반)
 * 
 * 역할:
 * - 중고거래 결제 시 중복 결제 방지를 위한 분산 락 제공
 * - TTL 10분으로 자동 만료
 * - SETNX를 통한 원자적 락 획득
 * - paymentKey와 usedTradeIdx 기반 중복 체크
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsedPaymentLockService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UsedPayRepository usedPayRepository;
    private final UsedTradeRepository usedTradeRepository;

    private static final String LOCK_PREFIX_PAYMENT_KEY = "lock:payment:key:";
    private static final String LOCK_PREFIX_TRADE = "lock:payment:trade:";
    private static final long LOCK_TTL_MINUTES = 10L; // 10분

    /**
     * 결제 락 생성 (paymentKey 기반 - 가장 안전)
     * 
     * @param usedTradeIdx 거래 ID
     * @param paymentKey 토스페이먼츠 결제 키
     * @param orderId 주문 ID
     * @param buyerIdx 구매자 ID
     * @return 락 생성 성공 여부 및 메시지
     */
    public UsedPaymentLockDto createLock(Integer usedTradeIdx, String paymentKey, 
                                         String orderId, Integer buyerIdx) {
        // 1단계: paymentKey 기반 중복 체크 (DB 먼저 확인)
        if (paymentKey != null && !paymentKey.isBlank()) {
            Optional<UsedPay> existingPayment = usedPayRepository.findByPaymentKey(paymentKey);
            if (existingPayment.isPresent()) {
                log.warn("이미 처리된 결제 키: paymentKey={}, usedPayIdx={}", 
                        paymentKey, existingPayment.get().getUsedPayIdx());
                return UsedPaymentLockDto.builder()
                        .success(false)
                        .message("이미 처리된 결제입니다.")
                        .build();
            }
        }

        // 2단계: usedTradeIdx 기반 중복 체크 (DB 확인)
        // 주의: 거래가 삭제되었을 수 있으므로, paymentKey 중복 체크가 우선
        // 거래가 존재하는 경우에만 추가 검증 수행
        if (usedTradeIdx != null) {
            Optional<UsedTrade> tradeOpt = usedTradeRepository.findById(usedTradeIdx);
            if (tradeOpt.isPresent()) {
                UsedTrade trade = tradeOpt.get();
                // 이미 거래완료 상태인지 확인
                if (trade.getStstus() != 0) { // 0 = 거래중, 1 = 거래완료, 2 = 거래취소
                    log.warn("이미 처리된 거래: usedTradeIdx={}, status={}", usedTradeIdx, trade.getStstus());
                    return UsedPaymentLockDto.builder()
                            .success(false)
                            .message("이미 처리된 거래입니다.")
                            .build();
                }

                // 해당 거래에 이미 완료된 결제가 있는지 확인
                var existingPayments = usedPayRepository.findByUsedTradeIdxOrderByCreatedAtDesc(usedTradeIdx);
                if (!existingPayments.isEmpty()) {
                    UsedPay latestPayment = existingPayments.get(0);
                    if (latestPayment.getStatus() == 1) { // 1 = 결제 완료
                        log.warn("이미 결제 완료된 거래: usedTradeIdx={}, usedPayIdx={}", 
                                usedTradeIdx, latestPayment.getUsedPayIdx());
                        return UsedPaymentLockDto.builder()
                                .success(false)
                                .message("이미 결제 완료된 거래입니다.")
                                .build();
                    }
                }
            } else {
                // 거래가 존재하지 않는 경우 (삭제되었을 수 있음)
                // 하지만 paymentKey 중복 체크는 이미 완료했으므로, 락은 획득 가능
                // 단, 로그만 남기고 계속 진행
                log.warn("거래가 존재하지 않음 (삭제되었을 수 있음): usedTradeIdx={}", usedTradeIdx);
                // 거래가 없어도 paymentKey가 unique하면 결제 진행 가능
            }
        }

        // 3단계: Redis 락 획득 (paymentKey 우선, 없으면 usedTradeIdx 사용)
        String lockKey;
        if (paymentKey != null && !paymentKey.isBlank()) {
            lockKey = LOCK_PREFIX_PAYMENT_KEY + paymentKey;
        } else if (usedTradeIdx != null) {
            lockKey = LOCK_PREFIX_TRADE + usedTradeIdx;
        } else {
            log.error("락 키 생성 실패: paymentKey와 usedTradeIdx가 모두 null");
            return UsedPaymentLockDto.builder()
                    .success(false)
                    .message("결제 정보가 올바르지 않습니다.")
                    .build();
        }

        try {
            // 락 데이터 구성
            Map<String, Object> lockData = new HashMap<>();
            lockData.put("usedTradeIdx", usedTradeIdx);
            lockData.put("paymentKey", paymentKey);
            lockData.put("orderId", orderId);
            if (buyerIdx != null) {
                lockData.put("buyerIdx", buyerIdx);
            }
            lockData.put("createdAt", LocalDateTime.now().toString());
            lockData.put("expireTime", LocalDateTime.now().plusMinutes(LOCK_TTL_MINUTES).toString());

            String lockValue = objectMapper.writeValueAsString(lockData);

            // SETNX: 키가 없을 때만 설정 (원자적 연산)
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, LOCK_TTL_MINUTES, TimeUnit.MINUTES);

            if (Boolean.TRUE.equals(acquired)) {
                log.info("결제 락 생성 성공: lockKey={}, usedTradeIdx={}, paymentKey={}, buyerIdx={}", 
                        lockKey, usedTradeIdx, paymentKey, buyerIdx);
                return UsedPaymentLockDto.builder()
                        .success(true)
                        .message("결제 락이 생성되었습니다.")
                        .expireTime(LocalDateTime.now().plusMinutes(LOCK_TTL_MINUTES))
                        .lockKey(lockKey)
                        .build();
            } else {
                log.warn("결제 락 생성 실패 (이미 존재): lockKey={}, usedTradeIdx={}", lockKey, usedTradeIdx);
                return UsedPaymentLockDto.builder()
                        .success(false)
                        .message("다른 요청이 이미 결제를 처리 중입니다. 잠시 후 다시 시도해주세요.")
                        .build();
            }

        } catch (JsonProcessingException e) {
            log.error("락 데이터 직렬화 실패: {}", e.getMessage(), e);
            return UsedPaymentLockDto.builder()
                    .success(false)
                    .message("결제 락 생성 중 오류가 발생했습니다.")
                    .build();
        } catch (Exception e) {
            log.error("결제 락 생성 중 예외 발생: {}", e.getMessage(), e);
            return UsedPaymentLockDto.builder()
                    .success(false)
                    .message("결제 락 생성 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 결제 락 해제
     * 
     * @param lockKey 락 키
     * @param buyerIdx 구매자 ID (소유권 검증용, 선택적)
     * @return 락 해제 성공 여부
     */
    public UsedPaymentLockDto releaseLock(String lockKey, Integer buyerIdx) {
        if (lockKey == null || lockKey.isBlank()) {
            log.warn("락 해제 시도: lockKey가 null");
            return UsedPaymentLockDto.builder()
                    .success(false)
                    .message("락 키가 올바르지 않습니다.")
                    .build();
        }

        try {
            String lockValue = redisTemplate.opsForValue().get(lockKey);

            if (lockValue == null) {
                log.info("결제 락 해제 시도: 락이 존재하지 않음 (이미 만료됨) - lockKey={}", lockKey);
                return UsedPaymentLockDto.builder()
                        .success(true)
                        .message("락이 이미 해제되었거나 만료되었습니다.")
                        .build();
            }

            // 락 소유권 검증 (선택적 - buyerIdx가 제공된 경우만)
            if (buyerIdx != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> lockData = objectMapper.readValue(lockValue, Map.class);
                    Integer lockOwner = (Integer) lockData.get("buyerIdx");

                    if (!buyerIdx.equals(lockOwner)) {
                        log.warn("결제 락 해제 실패: 소유권 불일치 - lockKey={}, requestBuyer={}, lockOwner={}",
                                lockKey, buyerIdx, lockOwner);
                        return UsedPaymentLockDto.builder()
                                .success(false)
                                .message("락 해제 권한이 없습니다.")
                                .build();
                    }
                } catch (Exception e) {
                    log.warn("락 소유권 검증 중 오류 (무시하고 진행): {}", e.getMessage());
                }
            }

            // 락 삭제
            Boolean deleted = redisTemplate.delete(lockKey);

            if (Boolean.TRUE.equals(deleted)) {
                log.info("결제 락 해제 성공: lockKey={}", lockKey);
                return UsedPaymentLockDto.builder()
                        .success(true)
                        .message("결제 락이 해제되었습니다.")
                        .build();
            } else {
                log.warn("결제 락 해제 실패: 삭제 실패 - lockKey={}", lockKey);
                return UsedPaymentLockDto.builder()
                        .success(false)
                        .message("락 해제에 실패했습니다.")
                        .build();
            }

        } catch (Exception e) {
            log.error("결제 락 해제 중 예외 발생: lockKey={}, error={}", lockKey, e.getMessage(), e);
            return UsedPaymentLockDto.builder()
                    .success(false)
                    .message("락 해제 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 락 존재 여부 확인
     * 
     * @param paymentKey 결제 키 또는 usedTradeIdx
     * @param isPaymentKey paymentKey인지 여부 (true: paymentKey, false: usedTradeIdx)
     * @return 락 존재 여부
     */
    public boolean isLocked(String key, boolean isPaymentKey) {
        String lockKey = isPaymentKey 
                ? LOCK_PREFIX_PAYMENT_KEY + key 
                : LOCK_PREFIX_TRADE + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * 락 정보 조회
     * 
     * @param lockKey 락 키
     * @return 락 정보 (없으면 null)
     */
    public Map<String, Object> getLockInfo(String lockKey) {
        String lockValue = redisTemplate.opsForValue().get(lockKey);

        if (lockValue == null) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> lockData = objectMapper.readValue(lockValue, Map.class);
            return lockData;
        } catch (JsonProcessingException e) {
            log.error("락 정보 역직렬화 실패: {}", e.getMessage(), e);
            return null;
        }
    }
}

