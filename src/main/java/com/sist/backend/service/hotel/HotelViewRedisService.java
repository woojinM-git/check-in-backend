package com.sist.backend.service.hotel;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;


import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 호텔 실시간 조회자 관리 서비스
 * 
 * - 세션당 TTL 3분
 * - 프론트엔드 sessionStorage 기반 세션 ID 사용
 * - 조회수 갱신 시 TTL 자동 갱신, 이탈 시 즉시 제거
 * - TTL 만료 시 자동 제거
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotelViewRedisService {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "hotel:view:";
    private static final long TTL_MINUTES = 3L; // TTL 3분

    /**
     * 호텔 상세 페이지 진입 시 Redis에 세션 등록 (신규 세션만)
     * 
     * - 신규 세션이면 등록하고 TTL 3분 설정
     * - 기존 세션이면 TTL만 갱신
     * 
     * @param contentId 호텔 고유 ID (DB에서 VARCHAR(50))
     * @param sessionId 프론트엔드 sessionStorage 기반 세션 ID
     */
    public void addActiveViewer(String contentId, String sessionId) {
        String key = PREFIX + contentId + ":" + sessionId;
        try {
            // 신규 세션이면 등록, 기존 세션이면 TTL 갱신
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.opsForValue().set(key, "1", TTL_MINUTES, TimeUnit.MINUTES);
                log.info("호텔 = {} 접속자 등록 (세션ID: {})", contentId, sessionId);
            } else {
                redisTemplate.expire(key, TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("호텔 = {} TTL 갱신 (세션ID: {})", contentId, sessionId);
            }
        } catch (Exception e) {
            log.error("Redis 등록 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 기존 세션의 TTL만 갱신 (이미 등록된 세션인 경우에만)
     * 
     * @param contentId 호텔 고유 ID
     * @param sessionId 브라우저 세션 ID
     */
    public void refreshViewerTTL(String contentId, String sessionId) {
        String key = PREFIX + contentId + ":" + sessionId;
        try {
            // 이미 등록된 세션인 경우에만 TTL 갱신
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.expire(key, TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("호텔 = {} TTL 갱신 (세션ID: {})", contentId, sessionId);
            }
        } catch (Exception e) {
            log.error("Redis TTL 갱신 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 현재 이 호텔 상세 페이지를 보고 있는 활성 접속자 조회
     *
     * @param contentId 호텔 ID
     * @return 현재 접속 인원 수
     */
    public int getActiveViewerCount(String contentId) {
        String pattern = PREFIX + contentId + ":*";
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            int count = (keys != null) ? keys.size() : 0;
            log.debug("호텔 = {} 현재 접속자 수 :{} (패턴: {})", contentId, count, pattern);
            return count;
        } catch (Exception e) {
            log.error("Redis 조회 중 오류: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 세션 만료 / 이탈 시 Redis에서 제거
     *
     * @param contentId 호텔 ID
     *                  @param sessionId 세션 ID
     */
    public void removeViewer(String contentId, String sessionId) {
        String key = PREFIX + contentId + ":" + sessionId;
        try {
            redisTemplate.delete(key);
            log.info("호텔 = {} 접속자 제거 (세션ID: {})", contentId, sessionId);
        } catch (Exception e) {
            log.error("Redis 세션 제거 중 오류: {}", e.getMessage(), e);
        }

    }
}
