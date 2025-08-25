package org.example.howareyou.domain.recommendationtag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.member.entity.MemberTag;
import org.example.howareyou.domain.recommendationtag.entity.MemberTagScore;
import org.example.howareyou.domain.recommendationtag.repository.MemberTagScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationTagService {

  private final TaggingNlpClient taggingNlpClient;
  private final MemberTagScoreRepository memberTagScoreRepository;

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

  /**
   * 프로필 관심사 기반 MemberTagScore 생성/업데이트
   * @param memberId 사용자 ID
   * @param interests 프로필에서 설정한 관심사들
   */
  @Transactional
  public void createOrUpdateMemberTagScores(Long memberId, Set<MemberTag> interests) {
    if (interests == null || interests.isEmpty()) {
      log.warn("관심사가 없어서 태그 점수를 생성할 수 없습니다: memberId={}", memberId);
      return;
    }

    // 기존 태그 점수 조회
    List<MemberTagScore> existingScores = memberTagScoreRepository.findByMemberId(memberId);
    Map<MemberTag, MemberTagScore> existingScoreMap = existingScores.stream()
        .collect(Collectors.toMap(MemberTagScore::getMemberTag, score -> score));

    // 각 관심사별로 점수 업데이트 또는 생성
    for (MemberTag interest : interests) {
      MemberTagScore tagScore = existingScoreMap.get(interest);
      
      if (tagScore != null) {
        // 기존 점수가 있으면 가중치 증가 (프로필에서 다시 선택했다는 것은 관심도 증가)
        double currentScore = tagScore.getScore();
        double newScore = Math.min(currentScore + 0.5, 5.0); // 최대 5.0점으로 제한
        tagScore.setScore(newScore);
        memberTagScoreRepository.save(tagScore);
        log.debug("기존 태그 점수 업데이트: memberId={}, tag={}, score: {} -> {}", 
                 memberId, interest, currentScore, newScore);
      } else {
        // 새로운 관심사면 기본 점수로 생성
        tagScore = new MemberTagScore();
        tagScore.setMemberId(memberId);
        tagScore.setMemberTag(interest);
        tagScore.setScore(1.0); // 기본 관심사 점수
        memberTagScoreRepository.save(tagScore);
        log.debug("새로운 태그 점수 생성: memberId={}, tag={}, score=1.0", memberId, interest);
      }
    }

    // 더 이상 선택되지 않은 관심사는 점수 감소 (완전 삭제하지 않음)
    for (MemberTagScore existingScore : existingScores) {
      if (!interests.contains(existingScore.getMemberTag())) {
        double currentScore = existingScore.getScore();
        double newScore = Math.max(currentScore - 0.3, 0.1); // 최소 0.1점으로 제한
        existingScore.setScore(newScore);
        memberTagScoreRepository.save(existingScore);
        log.debug("비선택 관심사 점수 감소: memberId={}, tag={}, score: {} -> {}", 
                 memberId, existingScore.getMemberTag(), currentScore, newScore);
      }
    }

    log.info("프로필 관심사 기반 태그 점수 업데이트 완료: memberId={}, interests={}", memberId, interests);
  }

  /**
   * 사용자 ID로 MemberTagScore 조회
   */
  public List<MemberTagScore> getMemberTagScores(Long memberId) {
    return memberTagScoreRepository.findByMemberId(memberId);
  }
}
