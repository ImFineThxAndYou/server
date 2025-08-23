package org.example.howareyou.domain.translate.service;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.translate.dto.TranslateRequestDto;
import org.example.howareyou.domain.translate.dto.TranslateResponseDto;
import org.example.howareyou.domain.translate.dto.LanguageDetectionResponseDto;
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
public class LiberTranslateService {
    @Value("${libretranslate.host}")
    private String host;

    @Value("${libretranslate.port}")
    private String port;

    private final RestTemplate restTemplate;
    /**
    * NLP Server(LiberTranslate)를 사용한 번역 메인 메소드입니다.
     * @param   requestDto  text,source_language,target_language가 포함되어있습니다.
     * @return  responseDto  번역된 text가 리턴됩니다.
     */
    public TranslateResponseDto translate(TranslateRequestDto requestDto){
        //request url 설정 (LiberTranslate endpoint)
        String url = "http://" + host + ":" + port+"/translate";
        //body 설정
        Map<String,Object> payload = Map.of(
                    "q",requestDto.getQ(),
                    "source",requestDto.getSource(),
                    "target",requestDto.getTarget(),
                    "format","text",
                    "api_key","" //로컬에서 서버를 띄었기에 api키는 필요하지 않음
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

    /**
     * LibreTranslate를 사용한 언어 감지 메서드
     * @param text 감지할 텍스트
     * @return LanguageDetectionResponseDto 감지된 언어 정보
     */
    public LanguageDetectionResponseDto detectLanguage(String text) {
        // LibreTranslate detect endpoint URL
        String url = "http://" + host + ":" + port + "/detect";
        
        // body 설정
        Map<String, Object> payload = Map.of(
            "q", text
        );
        
        // header 설정
        HttpHeaders detectHeaders = new HttpHeaders();
        detectHeaders.setContentType(MediaType.APPLICATION_JSON);
        
        // RestTemplate으로 언어 감지 요청 (배열 응답 처리)
        HttpEntity<Map<String, Object>> detectReq = new HttpEntity<>(payload, detectHeaders);
        ResponseEntity<Object[]> detectResp = restTemplate.postForEntity(url, detectReq, Object[].class);
        
        // 응답에서 언어 정보 추출 (배열의 첫 번째 요소)
        Object[] body = detectResp.getBody();
        if (body != null && body.length > 0) {
            @SuppressWarnings("unchecked")
            Map<String, Object> firstResult = (Map<String, Object>) body[0];
            
            String language = (String) firstResult.getOrDefault("language", "en");
            Object confidenceObj = firstResult.getOrDefault("confidence", 0.0);
            Double confidence = 0.0;
            
            if (confidenceObj instanceof Number) {
                confidence = ((Number) confidenceObj).doubleValue();
            }
            
            // 응답 DTO 생성 후 반환
            LanguageDetectionResponseDto responseDto = new LanguageDetectionResponseDto();
            responseDto.setLanguage(language);
            responseDto.setConfidence(confidence);
            return responseDto;
        }
        
        // 기본값 반환
        LanguageDetectionResponseDto responseDto = new LanguageDetectionResponseDto();
        responseDto.setLanguage("en");
        responseDto.setConfidence(0.0);
        return responseDto;
    }

    /**
     * 자동 언어 감지 및 번역 메서드
     * 텍스트의 언어를 자동으로 감지하고 반대 언어로 번역
     * @param requestDto 번역 요청 DTO (source, target 생략 가능)
     * @return TranslateResponseDto 번역된 텍스트
     */
    public TranslateResponseDto translateAuto(TranslateRequestDto requestDto) {
        String text = requestDto.getQ();
        
        // 1단계: 언어 자동 감지
        LanguageDetectionResponseDto detectedLang = detectLanguage(text);
        String sourceLang = detectedLang.getLanguage();
        
        // 2단계: 타겟 언어 자동 결정 (한국어 ↔ 영어)
        String targetLang;
        if ("ko".equals(sourceLang)) {
            targetLang = "en";
        } else {
            targetLang = "ko";
        }
        
        // 3단계: 번역 실행
        TranslateRequestDto autoRequest = new TranslateRequestDto();
        autoRequest.setQ(text);
        autoRequest.setSource(sourceLang);
        autoRequest.setTarget(targetLang);
        
        return translate(autoRequest);
    }
}
