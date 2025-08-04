package org.example.howareyou.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 계정 설정 응답 DTO
 */
@Getter
@Builder
public class MemberSettingsResponse {
    @Schema(
        description = "언어 설정 (ko: 한국어, en: 영어)", 
        example = "ko",
        allowableValues = {"ko", "en"}
    )
    private final String language;

    @Schema(
        description = "시간대 설정 (예: Asia/Seoul, UTC, America/New_York)", 
        example = "Asia/Seoul"
    )
    private final String timezone;
}
