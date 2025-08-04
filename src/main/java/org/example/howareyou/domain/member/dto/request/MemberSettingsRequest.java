package org.example.howareyou.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 계정 설정 업데이트 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class MemberSettingsRequest {

    @Schema(
        description = "언어 설정 (ko: 한국어, en: 영어)", 
        example = "ko",
        allowableValues = {"ko", "en"}
    )
    @Pattern(regexp = "(?i)^(ko|en)$", message = "지원하는 언어는 ko(한국어)와 en(영어)입니다.")
    private String language;

    @Schema(
        description = "시간대 설정 (예: Asia/Seoul, UTC, America/New_York)", 
        example = "Asia/Seoul"
    )
    private String timezone;
}
