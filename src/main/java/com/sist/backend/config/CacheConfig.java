package com.sist.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Spring Cache 설정
 * 지역별 통계 등 자주 조회되는 데이터를 캐싱하여 성능 최적화
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 인메모리 캐시 매니저 설정
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList("regionStatistics", "hotelRankings", "monthlyCommission"));
        return cacheManager;
    }
}

