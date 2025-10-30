package com.sist.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 특정 객실+날짜 조합에 락을 건다.
     *
     * @param roomId 객실 식별자
     * @param checkin 체크인 날짜
     * @param ttlSec 락 유지시간 (초)
     * @return 락 획득 성공 여부
     */

    //객실 단위 SoftRock TTL적용 10분으로
    public boolean tryLock(int roomId, LocalDate checkin,long ttlSec){
        String key = buildLockKey(roomId,checkin);
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key,"locked",ttlSec, TimeUnit.SECONDS);
        //성공일때
        if (Boolean.TRUE.equals(success)) {
            log.info("락 획득 성공: key={},ttl={}s", key, ttlSec);
            return true;
        }else {
            log.warn("락 획득 실패: key={}",key);
            return false;
        }
    }
    public void unlock(int roomId,LocalDate checkin){
        String key = buildLockKey(roomId,checkin);
        redisTemplate.delete(key);
        log.info("락 해제 완료 : key={} ",key);
    }
    //RedisKey생성규칙
    //ex) Lock:hotel:100234:2025-10-30
    private String buildLockKey(int roomId, LocalDate checkin){
        return String.format("lock:room:%d:%s",roomId,checkin.toString());
    }
}
