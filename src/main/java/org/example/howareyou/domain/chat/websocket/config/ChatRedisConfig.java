package org.example.howareyou.domain.chat.websocket.config;

import org.example.howareyou.domain.chat.websocket.entity.ChatMessageDocument;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class ChatRedisConfig {

  @Bean
  public RedisTemplate<String, ChatMessageDocument> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, ChatMessageDocument> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
  }

}