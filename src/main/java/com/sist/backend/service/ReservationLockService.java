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

    /**
     * 예약 락 생성 (SETNX 사용)
     *
     * @param customerIdx 고객 식별자
     * @param contentId 호텔 ID
     * @param roomId 객실 ID
     * @return 락 생성 성공 여부 및 메시지
     */
    public ReservationLockDto createLock(Integer customerIdx, String contentId, Integer roomId, String checkIn, String lockId) {
        String lockKey = LOCK_PREFIX + contentId + ":" + roomId + ":" + checkIn;

        try {
            // DB 사전 검증: 동일 일자 예약 존재 여부 확인
            if (checkIn == null || checkIn.isBlank()) {
                return ReservationLockDto.builder()
                        .success(false)
                        .message("checkIn은 필수입니다.")
                        .build();
            }

            boolean exists = roomReservationRepository.existsActiveReservation(
                    roomId, contentId, java.time.LocalDate.parse(checkIn)
            );
            if (exists) {
                log.warn("DB 예약 존재: roomId={}, contentId={}, checkIn={}", roomId, contentId, checkIn);
                return ReservationLockDto.builder()
                        .success(false)
                        .message("이미 예약 완료된 객실입니다.")
                        .build();
            }

            // 락 데이터 구성
            Map<String, Object> lockData = new HashMap<>();
            lockData.put("customerIdx", customerIdx);
            lockData.put("contentId", contentId);
            lockData.put("roomId", roomId);
            lockData.put("checkIn", checkIn);
            LocalDateTime createdAt = LocalDateTime.now();
            LocalDateTime expireAt = createdAt.plusMinutes(LOCK_TTL_MINUTES);
            String resolvedLockId = (lockId == null || lockId.isBlank()) ? UUID.randomUUID().toString() : lockId;
            lockData.put("lockId", resolvedLockId);
            lockData.put("createdAt", createdAt.toString());
            lockData.put("expireTime", expireAt.toString());

            String lockValue = objectMapper.writeValueAsString(lockData);

            // SETNX: 키가 없을 때만 설정 (원자적 연산)
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, LOCK_TTL_MINUTES, TimeUnit.MINUTES);

            if (Boolean.TRUE.equals(acquired)) {
                log.info("예약 락 생성 성공: roomId={}, contentId={}, checkIn={}, customerIdx={}", roomId, contentId, checkIn, customerIdx);
                return ReservationLockDto.builder()
                        .success(true)
                        .message("예약 락이 생성되었습니다.")
                        .expireTime(expireAt)
                        .lockKey(lockKey)
                        .lockId(resolvedLockId)
                        .build();
            } else {
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
    public ReservationLockDto releaseLock(String contentId, Integer roomId, String checkIn, Integer customerIdx, String lockId) {
        String lockKey = LOCK_PREFIX + contentId + ":" + roomId + ":" + checkIn;

        try {
            String lockValue = redisTemplate.opsForValue().get(lockKey);

            if (lockValue == null) {
                log.info("예약 락 해제 시도: 락이 존재하지 않음 (이미 만료됨) - roomId={}, contentId={}", roomId, contentId);
                return ReservationLockDto.builder()
                        .success(true)
                        .message("락이 이미 해제되었거나 만료되었습니다.")
                        .build();
            }

            // 락 소유권 검증 (선택적)
            @SuppressWarnings("unchecked")
            Map<String, Object> lockData = objectMapper.readValue(lockValue, Map.class);
            Integer lockOwner = (Integer) lockData.get("customerIdx");
            String storedLockId = (String) lockData.get("lockId");

            if (lockId != null && storedLockId != null && !lockId.equals(storedLockId)) {
                log.warn("예약 락 해제 실패: lockId 불일치 - roomId={}, contentId={}, requestLockId={}, storedLockId={}",
                        roomId, contentId, lockId, storedLockId);
                return ReservationLockDto.builder()
                        .success(false)
                        .message("락 해제 권한이 없습니다. (lockId 불일치)")
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
                log.info("예약 락 해제 성공: roomId={}, contentId={}, checkIn={}, customerIdx={}", roomId, contentId, checkIn, customerIdx);
                return ReservationLockDto.builder()
                        .success(true)
                        .message("예약 락이 해제되었습니다.")
                        .lockId(storedLockId)
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
    public boolean isLocked(String contentId, Integer roomId, String checkIn) {
        String lockKey = LOCK_PREFIX + contentId + ":" + roomId + ":" + checkIn;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * 락 정보 조회
     *
     * @param contentId 호텔 ID
     * @param roomId 객실 ID
     * @return 락 정보 (없으면 null)
     */
    public Map<String, Object> getLockInfo(String contentId, Integer roomId, String checkIn) {
        String lockKey = LOCK_PREFIX + contentId + ":" + roomId + ":" + checkIn;
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
