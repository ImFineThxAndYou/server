package org.example.howareyou.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "회원 테스트", description = "회원 관련 테스트용 API (개발 환경에서만 사용)")
public class MemberTestController {

  private final MemberRepository memberRepository;

  @Operation(
    summary = "전체 회원 목록 조회",
    description = "시스템에 등록된 모든 회원의 목록을 조회합니다. (테스트용)"
  )
  @GetMapping("/all")
  public List<Member> getAllMembers() {
    return memberRepository.findAll();
  }
}
