package org.example.howareyou.domain.translate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "번역 요청 DTO")
public class TranslateRequestDto {
    @Schema(description = "번역할 원문", example = "안녕하세요")
    private String q;
    @Schema(description = "소스 언어", example = "ko")
    private String source;
    @Schema(description = "타겟 언어", example = "en")
    private String target;
}
