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
    private static final long TTL_MINUTES = 3L; // TTL 5분
    private final StringRedisTemplate redisTemplate;

    /**
     * 호텔 상세 페이지 진입 시 Redus 에 세션 즉 활성 사용자 등록 TTL 1분
     *
     * 리팩터링 예정사항
     * TTL 3분으로 증가
     * 만료시 자동 제거
     * @param contentId 호텔 고유 ID (호텔 식별자,DB에선 VARCHAR(50))
     * @param sessionId 브라우저 세션 ID
     *
     * 추후 프로젝트 종료 후 파이프라인 배치 + SCAN 사용 예정
     */
    public void addActiveViewer(String contentId, String sessionId) {
        String key = PREFIX + contentId + ":" + sessionId;
        try {
            //존재하는지 여부 파악후 등록 된 세션일 경우 TTL 갱신
            if(Boolean.FALSE.equals(redisTemplate.hasKey(key))){
                // opsForValue Spring Data Redis에서 Redis의 문자열(String) 데티어 타입에 접근하여
                //값을 저장하는데 사용되는 객체를 반환하는 메서드 set으로 저장 get으로 값 조회
                redisTemplate.opsForValue().set(key,"1", TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("호텔 ={} 접속자 등록 (세션ID: {}", contentId,sessionId);
            }else {
                redisTemplate.expire(key,TTL_MINUTES,TimeUnit.MINUTES);
                log.debug("호텔 ={} TTL 갱신 (세션ID: {})", contentId,sessionId);
            }
        } catch (Exception e) {
            log.error("Redis 등록 중 오류: {}", e.getMessage(), e);
        }
        //값은 존재하면 1로 저장 TTL은 3분
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
