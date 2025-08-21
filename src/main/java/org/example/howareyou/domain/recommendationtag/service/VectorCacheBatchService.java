package org.example.howareyou.domain.recommendationtag.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.recommendationtag.repository.MemberTagScoreRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VectorCacheBatchService {

  private final MemberTagScoreRepository tagScoreRepository;
  private final RecommendationTagService tagService;
  private final MemberVectorRedisService redisService;

  private final static double weightB = 0.5;

  /** 모든 사용자 벡터 캐싱 (하루 1회) */
  public void cacheAllMemberVectors() {
    List<Long> allMemberIds = tagScoreRepository.findDistinctMemberIds();
    for (Long memberId : allMemberIds) {
      Map<String, Double> todayVector = tagService.refreshMemberScores(memberId);
      Map<String, Double> existing = redisService.getMemberVector(memberId);

      // 오늘 벡터 + 기존 벡터 가중 평균
      Map<String, Double> finalVector = blendVectors(existing, todayVector);

      redisService.saveMemberVector(memberId, finalVector);
    }
  }

  /** 단순 가중 평균 */
  private Map<String, Double> blendVectors(Map<String, Double> a, Map<String, Double> b) {
    Set<String> allKeys = new HashSet<>();
    allKeys.addAll(a.keySet());
    allKeys.addAll(b.keySet());

    double weightA = 1 - weightB;

    Map<String, Double> result = new HashMap<>();
    for (String key : allKeys) {
      double va = a.getOrDefault(key, 0.0);
      double vb = b.getOrDefault(key, 0.0);
      result.put(key, va * weightA + vb * weightB);
    }
    return result;
  }
}
