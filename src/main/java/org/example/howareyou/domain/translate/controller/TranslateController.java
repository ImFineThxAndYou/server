package org.example.howareyou.domain.translate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.translate.dto.TranslateRequestDto;
import org.example.howareyou.domain.translate.dto.TranslateResponseDto;
import org.example.howareyou.domain.translate.service.GeminiTranslateService;
import org.example.howareyou.domain.translate.service.LiberTranslateService;
import org.example.howareyou.global.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/translate")
@RequiredArgsConstructor
@Tag(name = "Translation", description = "텍스트 번역 API")
public class TranslateController {
    private final LiberTranslateService liberTranslateService;
    private final GeminiTranslateService geminiTranslateService;

    @Operation(
            summary = "기본 번역 (LiberTranslate)",
            description = "LiberTranslate 엔진을 사용해 텍스트를 번역합니다. 일반적으로 빠르고 경제적인 번역을 제공합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "번역 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TranslateResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/basic")
    public ResponseEntity<TranslateResponseDto> translateBasic(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "번역 요청 DTO",
                    required = true,
                    content = @Content(schema = @Schema(implementation = TranslateRequestDto.class))
            )
            TranslateRequestDto requestDto
    ){
        TranslateResponseDto responseDto = liberTranslateService.translate(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Gemini로 번역 (고품질/다시 번역)",
            description = "기본 번역 결과에 만족하지 않을 경우, Google Gemini API를 통해 고품질 번역을 제공합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "번역 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TranslateResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/specific")
    public ResponseEntity<TranslateResponseDto> translateSpecific(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "번역 요청 DTO",
                    required = true,
                    content = @Content(schema = @Schema(implementation = TranslateRequestDto.class))
            )
            TranslateRequestDto requestDto
    ){
        TranslateResponseDto responseDto = geminiTranslateService.translate(requestDto);
        return ResponseEntity.ok(responseDto);
    }
}
