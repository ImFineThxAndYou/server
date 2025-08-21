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

  private static final Map<String, MemberTag> TAG_TO_MEMBERTAG = Map.ofEntries(
      Map.entry("언어 학습", MemberTag.LANGUAGE_LEARNING),
      Map.entry("여행", MemberTag.TRAVEL),
      Map.entry("문화", MemberTag.CULTURE),
      Map.entry("비즈니스", MemberTag.BUSINESS),
      Map.entry("교육", MemberTag.EDUCATION),
      Map.entry("기술", MemberTag.TECHNOLOGY),
      Map.entry("스포츠", MemberTag.SPORTS),
      Map.entry("음악", MemberTag.MUSIC),
      Map.entry("음식", MemberTag.FOOD),
      Map.entry("예술", MemberTag.ART),
      Map.entry("과학", MemberTag.SCIENCE),
      Map.entry("역사", MemberTag.HISTORY),
      Map.entry("영화", MemberTag.MOVIES),
      Map.entry("게임", MemberTag.GAMES),
      Map.entry("문학", MemberTag.LITERATURE),
      Map.entry("사진", MemberTag.PHOTOGRAPHY),
      Map.entry("자연", MemberTag.NATURE),
      Map.entry("피트니스", MemberTag.FITNESS),
      Map.entry("패션", MemberTag.FASHION),
      Map.entry("봉사", MemberTag.VOLUNTEERING),
      Map.entry("동물", MemberTag.ANIMALS),
      Map.entry("자동차", MemberTag.CARS),
      Map.entry("DIY", MemberTag.DIY),
      Map.entry("금융", MemberTag.FINANCE)
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
