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
    Map<String, Double> baseVector = tagService.refreshMemberScores(memberId);
    if (baseVector.isEmpty()) {
      log.warn("태그 점수가 없음: memberId={}", memberId);
      return List.of();
    }

    // 전체 사용자 ID (자기 자신 제외)
    List<Long> allMemberIds = tagScoreRepository.findDistinctMemberIdByMemberIdNot(memberId);

    List<SimilarityResult> similarityResults = new ArrayList<>();

    for (Long otherId : allMemberIds) {
      Map<String, Double> otherVector = tagService.refreshMemberScores(otherId); // or 캐시된 점수 사용
      double similarity = cosineSimilarity(baseVector, otherVector);
      similarityResults.add(new SimilarityResult(otherId, similarity));
    }

    // 유사도 내림차순 정렬 후 상위 topN 추출
    return similarityResults.stream()
        .sorted(Comparator.comparingDouble(SimilarityResult::similarity).reversed())
        .limit(topN)
        .map(SimilarityResult::memberId)
        .toList();
  }
}