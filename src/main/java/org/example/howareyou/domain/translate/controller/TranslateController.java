package org.example.howareyou.domain.translate.controller;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.translate.dto.TranslateRequestDto;
import org.example.howareyou.domain.translate.dto.TranslateResponseDto;
import org.example.howareyou.domain.translate.service.TranslateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("ap1/v1/translate")
@RequiredArgsConstructor
public class TranslateController {
    private final TranslateService translateService;
    @PostMapping("")
    public ResponseEntity<?> translate(@RequestBody TranslateRequestDto requestDto){
        TranslateResponseDto responseDto = translateService.translate(requestDto);
        return ResponseEntity.ok(responseDto);
    }
}
