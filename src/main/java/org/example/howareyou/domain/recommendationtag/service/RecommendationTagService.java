package org.example.howareyou.domain.recommendationtag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.entity.MemberTag;
import org.example.howareyou.domain.recommendationtag.repository.MemberTagScoreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationTagService {

  private final TaggingNlpClient taggingNlpClient;

  private final MemberTagScoreRepository repository;

  // 필요시 매핑 테이블 사용 (파이썬이 Enum명으로 보내면 불필요)
  private static final Map<String, MemberTag> TAG_TO_MEMBERTAG = Map.of(
//      "음식 & 요리", MemberTag.FOOD_COOKING,
//      "여행 & 관광", MemberTag.TRAVEL_TOURISM
          /**
           * 지금 기존에 있는 memberTag 안 enum과 바꿀 내용 (FOOD_COOKING) 등등 맞지 않는 문제가 있기에
           * 임시로 주석처리
           */
  );

  /** 단어 리스트 → 태그 스코어 */
  public Map<String, Double> getTagScores(List<String> words) {
    if (words == null || words.isEmpty()) return Map.of();
    var scores = taggingNlpClient.classifyWords(words);
    log.debug("Tag scores for {}: {}", words, scores);
    return scores;
  }

  /** 멤버 단어장 기반 태깅 */
  public Map<String, Double> refreshMemberScores(long memberId) {
    var scores = taggingNlpClient.classifyMember(memberId);
    log.info("Refreshed tag scores for member {}: {}", memberId, scores);
    return scores;
  }

}
