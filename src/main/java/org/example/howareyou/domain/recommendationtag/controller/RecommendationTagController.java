package org.example.howareyou.domain.recommendationtag.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.recommendationtag.dto.TagScoresResponse;
import org.example.howareyou.domain.recommendationtag.dto.WordsRequest;
import org.example.howareyou.domain.recommendationtag.service.RecommendationMemberService;
import org.example.howareyou.domain.recommendationtag.service.RecommendationTagService;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Validated
public class RecommendationTagController {

  private final RecommendationTagService tagService;
  private final RecommendationMemberService memberService;

  /**
   * 단어 리스트 → 태그 스코어
   */
  @PostMapping("/classify")
  public TagScoresResponse classify(@Valid @RequestBody WordsRequest req) {
    return new TagScoresResponse(tagService.getTagScores(req.getWords()));
  }

  /**
   * 멤버 단어장 기반 태깅/저장
   */
  @PostMapping("/members/{memberId}/classify")
  public TagScoresResponse classifyMember(@PathVariable @Min(1) long memberId) {
    return new TagScoresResponse(tagService.refreshMemberScores(memberId));
  }

  @GetMapping("/recommend")
  public List<String> recommendMembers(
      @AuthenticationPrincipal CustomMemberDetails memberDetails,
      @RequestParam(defaultValue = "5") int topN) {

    Long memberId = memberDetails.getId(); // JWT 에서 꺼낸 사용자 ID
    return memberService.recommendSimilarMembers(memberId, topN);
  }

}