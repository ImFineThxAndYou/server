package org.example.howareyou.domain.recommendationtag.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.recommendationtag.dto.SimilarityResult;
import org.example.howareyou.domain.recommendationtag.entity.MemberTagScore;
import org.example.howareyou.domain.recommendationtag.repository.MemberTagScoreRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationMemberService {

  private final RecommendationTagService tagService;
  private final MemberTagScoreRepository tagScoreRepository;
  private final MemberVectorRedisService redisService;
  private final MemberService memberService;


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
  public List<String> recommendSimilarMembers(Long memberId, int topN) {
    // 1. 기준 사용자 벡터 가져오기 (Redis 우선, 없으면 MemberTagScore 기반)
    Map<String, Double> baseVector = getOrRefreshMemberVector(memberId);
    if (baseVector.isEmpty()) {
      log.warn("태그 점수가 없음: memberId={}", memberId);
      return List.of();
    }

    // 2. 전체 사용자 ID (자기 자신 제외) - MemberTagScore 테이블에서 조회
    List<Long> allMemberIds = tagScoreRepository.findDistinctMemberIdsExcept(memberId);
    
    if (allMemberIds.isEmpty()) {
      log.warn("추천할 수 있는 다른 사용자가 없음: memberId={}", memberId);
      return List.of();
    }

    List<SimilarityResult> similarityResults = new ArrayList<>();

    // 3. 각 사용자와 코사인 유사도 계산
    for (Long otherId : allMemberIds) {
      Map<String, Double> otherVector = getOrRefreshMemberVector(otherId);
      if (!otherVector.isEmpty()) {
        double similarity = cosineSimilarity(baseVector, otherVector);
        similarityResults.add(new SimilarityResult(otherId, similarity));
      }
    }

    // 4. 유사도 내림차순 정렬 후 상위 topN 추출, memberId를 membername으로 변환
    return similarityResults.stream()
        .sorted(Comparator.comparingDouble(SimilarityResult::similarity).reversed())
        .limit(topN)
        .map(result -> {
          try {
            return memberService.findMembernameById(result.memberId());
          } catch (Exception e) {
            log.warn("사용자 {}의 membername 조회 실패: {}", result.memberId(), e.getMessage());
            return "unknown_" + result.memberId(); // 폴백값
          }
        })
        .toList();
  }

  /**
   * Redis 캐시에서 벡터 조회 → 없으면 새로 계산 후 캐시에 저장
   * MemberTagScore가 있으면 기본 벡터 생성
   */
  private Map<String, Double> getOrRefreshMemberVector(Long memberId) {
    // 1. Redis 캐시에서 벡터 조회
    Map<String, Double> cached = redisService.getMemberVector(memberId);
    if (!cached.isEmpty()) {
      return cached;
    }
    
    // 2. FastAPI AI 태깅 시도
    Map<String, Double> fresh = tagService.refreshMemberScores(memberId);
    if (!fresh.isEmpty()) {
      redisService.saveMemberVector(memberId, fresh);
      return fresh;
    }
    
    // 3. MemberTagScore 기반 기본 벡터 생성 (폴백)
    List<MemberTagScore> tagScores = tagService.getMemberTagScores(memberId);
    if (!tagScores.isEmpty()) {
      Map<String, Double> basicVector = tagScores.stream()
          .collect(Collectors.toMap(
              score -> score.getMemberTag().name(),
              MemberTagScore::getScore
          ));
      
      // Redis에 기본 벡터 저장
      redisService.saveMemberVector(memberId, basicVector);
      log.info("MemberTagScore 기반 기본 벡터 생성: memberId={}, vector={}", memberId, basicVector);
      return basicVector;
    }
    
    log.warn("사용자 {}에 대한 태그 점수를 찾을 수 없습니다", memberId);
    return Map.of();
  }

}