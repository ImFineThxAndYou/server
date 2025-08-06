package org.example.howareyou.domain.translate.controller;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.translate.dto.TranslateRequestDto;
import org.example.howareyou.domain.translate.dto.TranslateResponseDto;
import org.example.howareyou.domain.translate.service.GeminiTranslateService;
import org.example.howareyou.domain.translate.service.LiberTranslateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/translate")
@RequiredArgsConstructor
public class TranslateController {
    private final LiberTranslateService liberTranslateService;
    private final GeminiTranslateService geminiTranslateService;

    /**
     * 번역시 기본값은 LiberTranslate을 이용합니다.
     */
    @PostMapping("/basic")
    public ResponseEntity<TranslateResponseDto> translateBasic(@RequestBody TranslateRequestDto requestDto){
        TranslateResponseDto responseDto = liberTranslateService.translate(requestDto);
        return ResponseEntity.ok(responseDto);
    }
    /**
     * 유저가 기본으로 제공되는 번역이 마음에 들지 않는다면, Gemini Api를 이용하여 번역을 제공합니다.
     */
    @PostMapping("/specific")
    public ResponseEntity<TranslateResponseDto> translateSpecific(@RequestBody TranslateRequestDto requestDto){
        TranslateResponseDto responseDto = geminiTranslateService.translate(requestDto);
        return ResponseEntity.ok(responseDto);
    }

}
