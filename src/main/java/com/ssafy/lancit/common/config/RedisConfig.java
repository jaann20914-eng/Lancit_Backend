package com.ssafy.lancit.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


// Redis : DB 앞에 세워둔 초고속 캐시 서버, 자주 쓰는 데이터는 여기서 먼저 꺼내고 없을 때만 DB 접근
// Redis config의 역할
// 1.Redis 서버에 연결
// 2. 연결된 Redis에 도구 등록
// 3. 


@Configuration
@EnableCaching
public class RedisConfig {

	// 1. localhost:6379 Redis 서버에 연결
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    
    // 2. 연결된 Redis 에 직접 읽기/쓰기 가능한 도구 등록 : (MailService, TaskScheduler 에서 주입받아 사용) , 문자열 저장/조회/삭제
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer()); //Redis 에 저장할 때 Key/Value 를 String 으로 직렬화
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    
    // @Cacheable, @CacheEvict 어노테이션이 동작하도록 등록
    // (HolidayService, FileService 에서 사용) 
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("signedUrl", defaultConfig.entryTtl(Duration.ofDays(6)));
        cacheConfigs.put("holiday",   defaultConfig.entryTtl(Duration.ofDays(365)));
        // 캐시 이름별로 TTL 개별 설정
        // signedUrl → 6일, holiday → 365일

        
        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
    
    // 채팅 pub/sub 리스너 컨테이너  -> 당장은 사용 x 나중에 고도화에 사용
	/*
	 * @Bean public RedisMessageListenerContainer redisMessageListenerContainer(
	 * RedisConnectionFactory factory) { RedisMessageListenerContainer container =
	 * new RedisMessageListenerContainer(); container.setConnectionFactory(factory);
	 * return container; }
	 */
    
}