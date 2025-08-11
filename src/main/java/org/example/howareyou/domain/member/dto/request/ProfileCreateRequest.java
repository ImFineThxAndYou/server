package org.example.howareyou.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.example.howareyou.domain.member.entity.Category;

import java.time.LocalDate;
import java.util.Set;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileCreateRequest {

    /* 필수 */
    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    /* 선택 */
    private String statusMessage;
    private String avatarUrl;
    private Set<Category> interests;

    /* 추가 정보 */
    private LocalDate birthDate;

    /**  ISO-3166 alpha-2  */
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "국가코드는 2자리입니다.")
    private String country;

    private String region;       // 시·도·주 등

    /* 로케일 */
    @Pattern(regexp = "^(ko|en)$", message = "지원되지 않는 언어입니다.")
    private String language;

    private String timezone;     // 예: Asia/Seoul
}