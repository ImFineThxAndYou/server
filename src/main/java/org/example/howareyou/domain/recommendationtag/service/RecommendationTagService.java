package org.example.howareyou.domain.recommendationtag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationTagService {

  private final TaggingNlpClient taggingNlpClient;

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
