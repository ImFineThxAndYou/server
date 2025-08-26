package org.example.howareyou.domain.auth.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.auth.dto.AuthDTO;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.repository.MemberRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthTestController {
  private final MemberRepository memberRepository;

  @PostMapping("/signup")
  public void signup(@RequestBody AuthDTO authDTO) {

    Member member = authDTO.toEntity();
    memberRepository.save(member);

  }

  @PostMapping("/signup/bulk")
  public void signupBulk(@RequestBody List<AuthDTO> authDTOs) {
    List<Member> members = authDTOs.stream()
        .map(AuthDTO::toEntity)
        .toList();
    memberRepository.saveAll(members);
  }

}
