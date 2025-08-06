package org.example.howareyou.domain.translate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "번역 응답 DTO")

public class TranslateResponseDto {
    @Schema(description = "번역된 결과", example = "Hello")
    String translatedText;
}
