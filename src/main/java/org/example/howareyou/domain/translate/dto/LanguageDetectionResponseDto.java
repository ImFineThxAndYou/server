package org.example.howareyou.domain.translate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "언어 감지 응답 DTO")
public class LanguageDetectionResponseDto {
    @Schema(description = "감지된 언어 코드", example = "ko")
    private String language;
    
    @Schema(description = "감지 신뢰도 (0.0 ~ 1.0)", example = "0.95")
    private Double confidence;
}
