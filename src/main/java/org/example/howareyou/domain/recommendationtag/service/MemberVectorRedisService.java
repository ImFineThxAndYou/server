package org.example.howareyou.domain.recommendationtag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberVectorRedisService {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  private static final String PREFIX = "member:vector:";

  public void saveMemberVector(Long memberId, Map<String, Double> vector) {
    String key = PREFIX + memberId;
    try {
      String json = objectMapper.writeValueAsString(vector);
      // TTL 24시간 설정
      redisTemplate.opsForValue().set(key, json, Duration.ofHours(24));
    } catch (JsonProcessingException e) {
      throw new CustomException(ErrorCode.VECTOR_SERIALIZATION_FAIL);
    }
  }


  public Map<String, Double> getMemberVector(Long memberId) {
    String key = PREFIX + memberId;
    String json = redisTemplate.opsForValue().get(key);
    if (json == null) return Map.of();
    log.debug("Redis에 memberId {} 벡터 저장 완료. TTL: 24시간", memberId);
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      log.warn("Redis에 memberId {}에 대한 벡터가 존재하지 않습니다.", memberId);
      throw new CustomException(ErrorCode.VECTOR_DESERIALIZATION_FAIL);

    }

  }

  public boolean hasVector(Long memberId) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + memberId));
  }

}
