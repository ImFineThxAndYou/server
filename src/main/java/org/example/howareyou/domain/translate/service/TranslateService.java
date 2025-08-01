package org.example.howareyou.domain.translate.service;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.translate.dto.TranslateRequestDto;
import org.example.howareyou.domain.translate.dto.TranslateResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TranslateService {
    @Value("${libretranslate.host}")
    private String host;

    @Value("${libretranslate.port}")
    private String port;

    private final RestTemplate restTemplate;
    public TranslateResponseDto translate(TranslateRequestDto requestDto){
        //request url 설정 (LiberTranslate endpoint)
        String url = "http://" + host + ":" + port+"/translate";
        //body 설정
        Map<String,Object> payload = Map.of(
                    "q",requestDto.getQ(),
                    "source",requestDto.getSource(),
                    "target",requestDto.getTarget(),
                    "format","text",
                    "api_key",""
            );
        //header 설정
        HttpHeaders transHeaders = new HttpHeaders();
        transHeaders.setContentType(MediaType.APPLICATION_JSON);

        //restTemplate으로 인한 request 날리기
        HttpEntity<Map<String, Object>> transReq = new HttpEntity<>(payload, transHeaders);
        ResponseEntity<Map> transResp = restTemplate.postForEntity(url, transReq, Map.class);

        //번역 text 추출
        Map body = transResp.getBody();
        String translatedText = (String) body.getOrDefault("translatedText","").toString().replace("\"", "");

        //응답 DTO 생성 후 반환
        TranslateResponseDto responseDto = new TranslateResponseDto();
        responseDto.setTranslatedText(translatedText);
        return responseDto;
    }
}
