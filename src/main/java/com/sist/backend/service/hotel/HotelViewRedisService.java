package com.sist.backend.service.hotel;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;


import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 호텔 실시간 조회자 관리 서비스 세션당 TTL1분 프론트가 나갔을 때 갱신되지 않으면 자동 제거됨
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotelViewRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String PREFIX = "hotel:view:";
    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * 호텔 상세 페이지 진입 시 Redus 에 세션 즉 활성 사용자 등록 TTL 1분
     *
     * @param contentId 호텔 고유 ID (호텔 식별자, DB에선 VARCHAR(50))
     * @param sessionId 브라우저 세션 ID
     */
    public void addActiveViewer(String contentId, String sessionId) {
        String key = PREFIX + contentId + ":" + sessionId;

        //값은 존재하면 1로 저장 TTL은 1분
        redisTemplate.opsForValue().set(key, "1", 1, TimeUnit.MINUTES);
        log.info("호텔 {}접속자 등록 (세션ID:{})", contentId, sessionId);
    }

    /**
     * 현재 이 호텔 상세 페이지를 보고 있는 활성 접속자 조회
     *
     * @param contentId 호텔 ID
     * @return 현재 접속 인원 수
     */
    public int getActiveViewerCount(String contentId) {
        String pattern = PREFIX + contentId + ":*";
        Set<Object> keys = redisTemplate.keys(pattern);
        int count = (keys != null) ? keys.size() : 0;
        log.info("호텔{} 현재 접속자 수 :{} (패턴: {})", contentId, count, pattern);
        return count;
    }

    /**
     * 세션 만료 / 이탈 시 Redis에서 제거
     */
    public void removeViewer(String contentId, String sessionId) {
        String key = PREFIX + contentId + ":" + sessionId;
        redisTemplate.delete(key);
        log.info("호텔 {} 접속자 제거 (세션ID: {})", contentId, sessionId);
    }
}
