package com.sist.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 문자열 데이터 처리를 위한 기본 설정
 * -RedisConnectionFactory는 Spirng Boot가 자동으로 주입해줌
 * -문자열 저장시 한글/특문 깨짐 방지
*/
@Configuration
public class RedisConfig {
    //의존성 주입
    //RedisConnectionFactory은 수동 등록이고  스프링 부트 3.5이후에는
    //StringRedisTemplate 자동 주입 사용함
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory){
        StringRedisTemplate template = new StringRedisTemplate(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());

        return template;
    }
}
