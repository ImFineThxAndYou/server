package org.example.howareyou.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.Role;

@Getter
@Builder

public class AuthDTO {
  private final Long memberId;
  private final String membername;
  private final String email;
  private final String role;
  private final boolean active;

  // DTO → Entity 변환
  public Member toEntity() {
    return Member.builder()             // Member 엔티티에 @Builder가 있다고 가정
        .membername(membername)
        .email(email)
        .role(Role.valueOf(role))
        .active(active)
        .build();
  }

  @JsonCreator
  public AuthDTO(@JsonProperty("memberId") Long memberId,
      @JsonProperty("membername") String membername,
      @JsonProperty("email") String email,
      @JsonProperty("role") String role,
      @JsonProperty("active") boolean active) {
    this.memberId = memberId;
    this.membername = membername;
    this.email = email;
    this.role = role;
    this.active = active;
  }
}