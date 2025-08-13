package org.example.howareyou.domain.member.controller;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberTestController {

  private final MemberRepository memberRepository;

  // 전체 회원 목록 조회
  @GetMapping("/all")
  public List<Member> getAllMembers() {
    return memberRepository.findAll();
  }
}
