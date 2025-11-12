package com.sist.backend.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sist.backend.dto.ReservationLockDto;
import com.sist.backend.repository.RoomReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 예약 락 관리 서비스 (Redis 기반)
 *
 * 역할: - 호텔 객실 예약 시 중복 예약 방지를 위한 분산 락 제공 - TTL 10분으로 자동 만료 - SETNX를 통한 원자적 락 획득
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationLockService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RoomReservationRepository roomReservationRepository;

    private static final String LOCK_PREFIX = "lock:room:";
    private static final long LOCK_TTL_MINUTES = 10L; // 10분
    private static final long LOCK_WINDOW_SECONDS = TimeUnit.MINUTES.toSeconds(LOCK_TTL_MINUTES);
    private static final long MIN_RENEWAL_SECONDS = 180L; // 최소 3분은 보장

    /**
     * 예약 락 생성 (SETNX 사용)
     *
     * @param customerIdx 고객 식별자
     * @param contentId 호텔 ID
     * @param roomId 객실 ID
     * @return 락 생성 성공 여부 및 메시지
     */
    public ReservationLockDto createLock(
            Integer customerIdx,
            String contentId,
            Integer roomId,
            String checkIn,
            String checkOut,
            String lockId,
            String sessionId,
            String tabId,
            String initialLockAt
    ) {
        String lockKey = LOCK_PREFIX + contentId + ":" + roomId + ":" + checkIn + ":" + checkOut;

        try {
            // DB 사전 검증: 동일 일자 예약 존재 여부 확인
            if (checkIn == null || checkIn.isBlank()) {
                return ReservationLockDto.builder()
                        .success(false)
                        .message("checkIn은 필수입니다.")
                        .build();
            }

            if (checkOut == null || checkOut.isBlank()) {
                return ReservationLockDto.builder()
                        .success(false)
                        .message("checkOut은 필수입니다.")
                        .build();
            }

            boolean exists = roomReservationRepository.existsActiveReservationInRange(
                    roomId,
                    contentId,
                    java.time.LocalDate.parse(checkIn),
                    java.time.LocalDate.parse(checkOut)
            );
            if (exists) {
                log.warn("DB 예약 존재: roomId={}, contentId={}, checkIn={}, checkOut={}", roomId, contentId, checkIn, checkOut);
                return ReservationLockDto.builder()
                        .success(false)
                        .message("이미 예약 완료된 기간입니다.")
                        .build();
            }

            // 기존 락과 기간 겹침 확인
            String lockPrefix = LOCK_PREFIX + contentId + ":" + roomId + ":";
            java.time.LocalDate newCheckInDate = java.time.LocalDate.parse(checkIn);
            java.time.LocalDate newCheckOutDate = java.time.LocalDate.parse(checkOut);

            var existingLockKeys = redisTemplate.keys(lockPrefix + "*");
            if (existingLockKeys != null) {
                for (String existingKey : existingLockKeys) {
                    if (existingKey.equals(lockKey)) {
                        continue;
                    }
                    String existingValue = redisTemplate.opsForValue().get(existingKey);
                    if (existingValue == null) {
                        continue;
                    }
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> existingData = objectMapper.readValue(existingValue, Map.class);
                        String existingCheckIn = (String) existingData.get("checkIn");
                        String existingCheckOut = (String) existingData.get("checkOut");
                        if (existingCheckIn == null || existingCheckOut == null) {
                            continue;
                        }
                        java.time.LocalDate existingCheckInDate = java.time.LocalDate.parse(existingCheckIn);
                        java.time.LocalDate existingCheckOutDate = java.time.LocalDate.parse(existingCheckOut);

                        boolean overlaps
                                = newCheckInDate.isBefore(existingCheckOutDate)
                                && newCheckOutDate.isAfter(existingCheckInDate);

                        if (overlaps) {
                            log.warn("Redis 락 존재: roomId={}, contentId={}, new({}-{}), existingKey={}", roomId, contentId, checkIn, checkOut, existingKey);
                            return ReservationLockDto.builder()
                                    .success(false)
                                    .message("이미 다른 사용자가 동일 기간에 예약 진행 중입니다.")
                                    .build();
                        }
                    } catch (Exception ex) {
                        log.warn("기존 락 정보 파싱 실패(무시): key={}, error={}", existingKey, ex.getMessage());
                    }
                }
            }

            LocalDateTime now = LocalDateTime.now();

            String existingLockValue = redisTemplate.opsForValue().get(lockKey);
            if (existingLockValue != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> existingData = objectMapper.readValue(existingLockValue, Map.class);
                String storedLockId = (String) existingData.get("lockId");
                String storedSessionId = (String) existingData.get("sessionId");
                String storedTabId = (String) existingData.get("tabId");
                String storedInitialLockAtStr = (String) existingData.getOrDefault("initialLockAt", existingData.get("createdAt"));

                LocalDateTime requestedInitialLockAt = null;
                if (initialLockAt != null && !initialLockAt.isBlank()) {
                    try {
                        requestedInitialLockAt = LocalDateTime.parse(initialLockAt);
                    } catch (Exception ex) {
                        log.warn("initialLockAt 파싱 실패(요청값 무시): value={}, error={}", initialLockAt, ex.getMessage());
                        requestedInitialLockAt = null;
                    }
                }

                LocalDateTime storedInitialLockAt = null;
                if (storedInitialLockAtStr != null) {
                    try {
                        storedInitialLockAt = LocalDateTime.parse(storedInitialLockAtStr);
                    } catch (Exception ignored) {
                        storedInitialLockAt = null;
                    }
                }
                if (storedInitialLockAt == null) {
                    storedInitialLockAt = now;
                }

                LocalDateTime effectiveInitial = storedInitialLockAt;
                if (requestedInitialLockAt != null && requestedInitialLockAt.isAfter(storedInitialLockAt.minusSeconds(1))) {
                    effectiveInitial = requestedInitialLockAt;
                }

                long elapsedSeconds = java.time.Duration.between(effectiveInitial, now).getSeconds();
                if (elapsedSeconds < 0) {
                    elapsedSeconds = 0;
                }

                if (elapsedSeconds >= LOCK_WINDOW_SECONDS) {
                    redisTemplate.delete(lockKey);
                    log.info("기존 예약 락 만료 처리: roomId={}, contentId={}, lockId={}, elapsedSeconds={}", roomId, contentId, storedLockId, elapsedSeconds);
                } else {
                    boolean sameOwner = false;
                    if (lockId != null && storedLockId != null && lockId.equals(storedLockId)) {
                        sameOwner = true;
                    } else if (sessionId != null && storedSessionId != null
                            && tabId != null && storedTabId != null
                            && sessionId.equals(storedSessionId)
                            && tabId.equals(storedTabId)) {
                        sameOwner = true;
                    }

                    if (sameOwner) {
                        long remainingSeconds = LOCK_WINDOW_SECONDS - elapsedSeconds;
                        if (remainingSeconds <= 0 || remainingSeconds < MIN_RENEWAL_SECONDS) {
                            remainingSeconds = LOCK_WINDOW_SECONDS;
                            effectiveInitial = now;
                        }

                        existingData.put("sessionId", sessionId != null ? sessionId : storedSessionId);
                        existingData.put("tabId", tabId != null ? tabId : storedTabId);
                        existingData.put("initialLockAt", effectiveInitial.toString());
                        existingData.put("expireTime", effectiveInitial.plusSeconds(LOCK_WINDOW_SECONDS).toString());

                        redisTemplate.opsForValue().set(
                                lockKey,
                                objectMapper.writeValueAsString(existingData),
                                remainingSeconds,
                                TimeUnit.SECONDS
                        );

                        log.info("예약 락 재확인: roomId={}, contentId={}, lockId={}, remainingSeconds={}, sessionId={}, tabId={}",
                                roomId, contentId, storedLockId, remainingSeconds, sessionId, tabId);

                        return ReservationLockDto.builder()
                                .success(true)
                                .message("기존 예약 락을 유지합니다.")
                                .expireTime(effectiveInitial.plusSeconds(LOCK_WINDOW_SECONDS))
                                .lockKey(lockKey)
                                .checkOut(checkOut)
                                .lockId(storedLockId)
                                .initialLockAt(effectiveInitial.toString())
                                .build();
                    } else {
                        log.warn("예약 락 재요청 충돌: roomId={}, contentId={}, requestLockId={}, storedLockId={}, sessionId={}, tabId={}",
                                roomId, contentId, lockId, storedLockId, sessionId, tabId);
                        return ReservationLockDto.builder()
                                .success(false)
                                .message("이미 다른 사용자가 예약 진행 중입니다. 잠시 후 다시 시도해주세요.")
                                .build();
                    }
                }
            }

            // 락 데이터 구성
            LocalDateTime resolvedInitialLockAt = now;
            if (initialLockAt != null && !initialLockAt.isBlank()) {
                try {
                    resolvedInitialLockAt = LocalDateTime.parse(initialLockAt);
                } catch (Exception ex) {
                    log.warn("initialLockAt 파싱 실패(무시): value={}, error={}", initialLockAt, ex.getMessage());
                    resolvedInitialLockAt = now;
                }
            }
            if (resolvedInitialLockAt.isAfter(now)) {
                resolvedInitialLockAt = now;
            }

            long elapsedSecondsForNewLock = java.time.Duration.between(resolvedInitialLockAt, now).getSeconds();
            if (elapsedSecondsForNewLock < 0) {
                elapsedSecondsForNewLock = 0;
            }
            long ttlSeconds = LOCK_WINDOW_SECONDS - elapsedSecondsForNewLock;
            if (ttlSeconds <= 0) {
                ttlSeconds = LOCK_WINDOW_SECONDS;
                resolvedInitialLockAt = now;
            }
            LocalDateTime expireAt = resolvedInitialLockAt.plusSeconds(LOCK_WINDOW_SECONDS);
            Map<String, Object> lockData = new HashMap<>();
            lockData.put("customerIdx", customerIdx);
            lockData.put("contentId", contentId);
            lockData.put("roomId", roomId);
            lockData.put("checkIn", checkIn);
            lockData.put("checkOut", checkOut);
            LocalDateTime createdAt = now;
            String resolvedLockId = (lockId == null || lockId.isBlank()) ? UUID.randomUUID().toString() : lockId;
            lockData.put("lockId", resolvedLockId);
            lockData.put("sessionId", sessionId);
            lockData.put("tabId", tabId);
            lockData.put("initialLockAt", resolvedInitialLockAt.toString());
            lockData.put("createdAt", createdAt.toString());
            lockData.put("expireTime", expireAt.toString());

            String lockValue = objectMapper.writeValueAsString(lockData);

            // SETNX: 키가 없을 때만 설정 (원자적 연산)
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, ttlSeconds, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(acquired)) {
                log.info("예약 락 생성 성공: roomId={}, contentId={}, checkIn={}, sessionId={}, tabId={}, lockId={}",
                        roomId, contentId, checkIn, sessionId, tabId, resolvedLockId);
                return ReservationLockDto.builder()
                        .success(true)
                        .message("예약 락이 생성되었습니다.")
                        .expireTime(expireAt)
                        .lockKey(lockKey)
                        .checkOut(checkOut)
                        .lockId(resolvedLockId)
                        .initialLockAt(resolvedInitialLockAt.toString())
                        .build();
            } else {
                String currentLockValue = redisTemplate.opsForValue().get(lockKey);
                if (currentLockValue != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> currentData = objectMapper.readValue(currentLockValue, Map.class);
                        String currentLockId = (String) currentData.get("lockId");
                        String currentSessionId = (String) currentData.get("sessionId");
                        String currentTabId = (String) currentData.get("tabId");
                        String currentInitial = (String) currentData.get("initialLockAt");

                        boolean sameOwner
                                = (currentLockId != null && currentLockId.equals(lockId))
                                || (currentSessionId != null && currentTabId != null
                                && currentSessionId.equals(sessionId)
                                && currentTabId.equals(tabId));

                        if (sameOwner) {
                            log.info("예약 락 생성 재시도: 기존 락 유지 - roomId={}, contentId={}, lockId={}", roomId, contentId, currentLockId);
                            LocalDateTime responseExpire = null;
                            if (currentInitial != null) {
                                try {
                                    responseExpire = LocalDateTime.parse(currentInitial).plusSeconds(LOCK_WINDOW_SECONDS);
                                } catch (Exception ignored) {
                                    responseExpire = null;
                                }
                            }
                            return ReservationLockDto.builder()
                                    .success(true)
                                    .message("기존 예약 락을 유지합니다.")
                                    .expireTime(responseExpire)
                                    .lockKey(lockKey)
                                    .checkOut(checkOut)
                                    .lockId(currentLockId)
                                    .initialLockAt(currentInitial)
                                    .build();
                        }
                    } catch (Exception parseEx) {
                        log.warn("기존 락 정보 파싱 실패(무시): {}", parseEx.getMessage());
                    }
                }

                log.warn("예약 락 생성 실패 (이미 존재): roomId={}, contentId={}, checkIn={}", roomId, contentId, checkIn);
                return ReservationLockDto.builder()
                        .success(false)
                        .message("이미 다른 사용자가 예약 진행 중입니다. 잠시 후 다시 시도해주세요.")
                        .build();
            }

        } catch (JsonProcessingException e) {
            log.error("락 데이터 직렬화 실패: {}", e.getMessage(), e);
            return ReservationLockDto.builder()
                    .success(false)
                    .message("예약 락 생성 중 오류가 발생했습니다.")
                    .build();
        } catch (Exception e) {
            log.error("예약 락 생성 중 예외 발생: {}", e.getMessage(), e);
            return ReservationLockDto.builder()
                    .success(false)
                    .message("예약 락 생성 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 예약 락 해제
     *
     * @param contentId 호텔 ID
     * @param roomId 객실 ID
     * @param customerIdx 고객 식별자 (소유권 검증용)
     * @return 락 해제 성공 여부
     */
    public ReservationLockDto releaseLock(
            String contentId,
            Integer roomId,
            String checkIn,
            String checkOut,
            Integer customerIdx,
            String lockId,
            String sessionId,
            String tabId
    ) {
        String lockKey = LOCK_PREFIX + contentId + ":" + roomId + ":" + checkIn + ":" + checkOut;

        try {
            String lockValue = redisTemplate.opsForValue().get(lockKey);

            if (lockValue == null) {
                log.info("예약 락 해제 시도: 락이 존재하지 않음 (이미 만료됨) - roomId={}, contentId={}", roomId, contentId);
                return ReservationLockDto.builder()
                        .success(true)
                        .message("락이 이미 해제되었거나 만료되었습니다.")
                        .checkOut(checkOut)
                        .build();
            }

            // 락 소유권 검증 (선택적)
            @SuppressWarnings("unchecked")
            Map<String, Object> lockData = objectMapper.readValue(lockValue, Map.class);
            Integer lockOwner = (Integer) lockData.get("customerIdx");
            String storedLockId = (String) lockData.get("lockId");
            String storedCheckOut = (String) lockData.get("checkOut");
            String storedSessionId = (String) lockData.get("sessionId");
            String storedTabId = (String) lockData.get("tabId");
            String storedInitialLockAt = (String) lockData.get("initialLockAt");

            if (storedCheckOut != null && checkOut != null && !storedCheckOut.equals(checkOut)) {
                log.warn("예약 락 해제 실패: checkOut 불일치 - roomId={}, contentId={}, requestCheckOut={}, storedCheckOut={}",
                        roomId, contentId, checkOut, storedCheckOut);
                return ReservationLockDto.builder()
                        .success(false)
                        .message("락 해제 권한이 없습니다. (기간 불일치)")
                        .build();
            }

            if (lockId != null && storedLockId != null && !lockId.equals(storedLockId)) {
                log.warn("예약 락 해제 실패: lockId 불일치 - roomId={}, contentId={}, requestLockId={}, storedLockId={}",
                        roomId, contentId, lockId, storedLockId);
                return ReservationLockDto.builder()
                        .success(false)
                        .message("락 해제 권한이 없습니다. (lockId 불일치)")
                        .build();
            }

            if (sessionId != null && storedSessionId != null && !sessionId.equals(storedSessionId)) {
                log.warn("예약 락 해제 실패: sessionId 불일치 - roomId={}, contentId={}, requestSessionId={}, storedSessionId={}",
                        roomId, contentId, sessionId, storedSessionId);
                return ReservationLockDto.builder()
                        .success(false)
                        .message("락 해제 권한이 없습니다. (sessionId 불일치)")
                        .build();
            }

            if (tabId != null && storedTabId != null && !tabId.equals(storedTabId)) {
                log.warn("예약 락 해제 실패: tabId 불일치 - roomId={}, contentId={}, requestTabId={}, storedTabId={}",
                        roomId, contentId, tabId, storedTabId);
                return ReservationLockDto.builder()
                        .success(false)
                        .message("락 해제 권한이 없습니다. (tabId 불일치)")
                        .build();
            }

            if (customerIdx != null && lockOwner != null && !customerIdx.equals(lockOwner)) {
                log.warn("예약 락 해제 실패: 소유권 불일치 - roomId={}, contentId={}, requestCustomer={}, lockOwner={}",
                        roomId, contentId, customerIdx, lockOwner);
                return ReservationLockDto.builder()
                        .success(false)
                        .message("락 해제 권한이 없습니다.")
                        .build();
            }

            // 락 삭제
            Boolean deleted = redisTemplate.delete(lockKey);

            if (Boolean.TRUE.equals(deleted)) {
                log.info("예약 락 해제 성공: roomId={}, contentId={}, checkIn={}, sessionId={}, tabId={}, lockId={}",
                        roomId, contentId, checkIn, storedSessionId, storedTabId, storedLockId);
                return ReservationLockDto.builder()
                        .success(true)
                        .message("예약 락이 해제되었습니다.")
                        .checkOut(checkOut)
                        .lockId(storedLockId)
                        .initialLockAt(storedInitialLockAt)
                        .build();
            } else {
                log.warn("예약 락 해제 실패: 삭제 실패 - roomId={}, contentId={}, checkIn={}", roomId, contentId, checkIn);
                return ReservationLockDto.builder()
                        .success(false)
                        .message("락 해제에 실패했습니다.")
                        .build();
            }

        } catch (Exception e) {
            log.error("예약 락 해제 중 예외 발생: roomId={}, contentId={}, error={}", roomId, contentId, e.getMessage(), e);
            return ReservationLockDto.builder()
                    .success(false)
                    .message("락 해제 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 락 존재 여부 확인
     *
     * @param contentId 호텔 ID
     * @param roomId 객실 ID
     * @return 락 존재 여부
     */
    public boolean isLocked(String contentId, Integer roomId, String checkIn, String checkOut) {
        String lockKey = LOCK_PREFIX + contentId + ":" + roomId + ":" + checkIn + ":" + checkOut;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * 락 정보 조회
     *
     * @param contentId 호텔 ID
     * @param roomId 객실 ID
     * @return 락 정보 (없으면 null)
     */
    public Map<String, Object> getLockInfo(String contentId, Integer roomId, String checkIn, String checkOut) {
        String lockKey = LOCK_PREFIX + contentId + ":" + roomId + ":" + checkIn + ":" + checkOut;
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
