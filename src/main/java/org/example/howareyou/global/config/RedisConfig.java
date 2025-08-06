package org.example.howareyou.global.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        // 1) ObjectMapper 커스터마이징
        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())               // ★ 필수
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY); // 타입 정보 포함

        // 2) Serializer 생성
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(om);

        // 3) 템플릿에 Serializer 주입
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}