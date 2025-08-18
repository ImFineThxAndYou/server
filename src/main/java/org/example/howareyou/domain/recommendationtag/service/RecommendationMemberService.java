package org.example.howareyou.domain.recommendationtag.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.recommendationtag.dto.SimilarityResult;
import org.example.howareyou.domain.recommendationtag.repository.MemberTagScoreRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationMemberService {

  private final RecommendationTagService tagService;
  private final MemberTagScoreRepository tagScoreRepository;
  private final MemberVectorRedisService redisService;


  /**
   * 코사인 유사도 계산
   */
  private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
    Set<String> keys = new HashSet<>();
    keys.addAll(a.keySet());
    keys.addAll(b.keySet());

    double dot = 0.0, normA = 0.0, normB = 0.0;

    for (String key : keys) {
      double va = a.getOrDefault(key, 0.0);
      double vb = b.getOrDefault(key, 0.0);
      dot += va * vb;
      normA += va * va;
      normB += vb * vb;
    }

    return (normA == 0 || normB == 0) ? 0.0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
  }

  /**
   * 현재 사용자와 유사한 사용자 추천
   * @param memberId 현재 사용자 ID
   * @param topN 추천 인원 수
   */
  public List<Long> recommendSimilarMembers(Long memberId, int topN) {
    // 1. 기준 사용자 벡터 가져오기 (Redis 우선)
    Map<String, Double> baseVector = getOrRefreshMemberVector(memberId);
    if (baseVector.isEmpty()) {
      log.warn("태그 점수가 없음: memberId={}", memberId);
      return List.of();
    }

    // 2. 전체 사용자 ID (자기 자신 제외)
    List<Long> allMemberIds = tagScoreRepository.findDistinctMemberIdByMemberIdNot(memberId);

    List<SimilarityResult> similarityResults = new ArrayList<>();

    // 3. 각 사용자와 코사인 유사도 계산
    for (Long otherId : allMemberIds) {
      Map<String, Double> otherVector = getOrRefreshMemberVector(otherId);
      if (!otherVector.isEmpty()) {
        double similarity = cosineSimilarity(baseVector, otherVector);
        similarityResults.add(new SimilarityResult(otherId, similarity));
      }
    }

    // 4. 유사도 내림차순 정렬 후 상위 topN 추출
    return similarityResults.stream()
        .sorted(Comparator.comparingDouble(SimilarityResult::similarity).reversed())
        .limit(topN)
        .map(SimilarityResult::memberId)
        .toList();
  }

  /**
   * Redis 캐시에서 벡터 조회 → 없으면 새로 계산 후 캐시에 저장
   */
  private Map<String, Double> getOrRefreshMemberVector(Long memberId) {
    Map<String, Double> cached = redisService.getMemberVector(memberId);
    if (!cached.isEmpty()) {
      return cached;
    }
    Map<String, Double> fresh = tagService.refreshMemberScores(memberId);
    if (!fresh.isEmpty()) {
      redisService.saveMemberVector(memberId, fresh);
    }
    return fresh;
  }

}