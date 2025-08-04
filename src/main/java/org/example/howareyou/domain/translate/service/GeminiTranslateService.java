package org.example.howareyou.domain.translate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.translate.dto.TranslateRequestDto;
import org.example.howareyou.domain.translate.dto.TranslateResponseDto;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gemini API를 사용해 텍스트 번역(예: 영어 ↔ 한국어)을 수행하는 서비스.
 * <p>
 * 현재 기본 모델은 configuration 상의 {@code gemini.model}을 사용하며,
 * 그에 따라 적절한 suffix (:generateText 또는 :generateContent)를 자동 결정해 호출한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiTranslateService {

    @Value("${gemini.base-url}")
    private String baseUrl; // 예: https://generativelanguage.googleapis.com/v1

    @Value("${gemini.model}")
    private String model; // ex: "gemini-1.5" 또는 "gemini-2.5-flash"

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    /**
     * Gemini API를 호출해 번역 결과를 반환한다.
     *
     * @param requestDto 원문, 소스/타겟 언어 정보
     * @return 번역된 텍스트를 담은 DTO
     */
    public TranslateResponseDto translate(TranslateRequestDto requestDto) {
        String q = requestDto.getQ();
        String source = requestDto.getSource();
        String target = requestDto.getTarget();

        // 모델이 무엇인지에 따라 적절한 액션 suffix 결정
        String actionSuffix = model.contains("flash") ? ":generateContent" : ":generateText";
        String endpoint = String.format("%s/models/%s%s", baseUrl, model, actionSuffix);
        String url = UriComponentsBuilder.fromHttpUrl(endpoint).toUriString();

        // 문맥을 모델에게 전달 (번역 의도 명시)
        String context = String.format("Translate \"%s\" from %s to %s, en is english and ko is korean.", q, source, target);

        log.info("Gemini API 호출 시작. model={}, url={}, context={}", model, url, context);

        // 요청 페이로드 / 헤더 구성
        Map<String, Object> payload = buildPayload(context);
        HttpHeaders headers = buildHeaders();

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        String translatedText;

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            translatedText = parseResponse(response);
        } catch (Exception e) {
            log.error("Gemini API 호출 실패", e);
            throw new CustomException(ErrorCode.GM_SERVICE_UNAVAILABLE);
        }
        //불필요한 정보, 탈출 문자 제거
        translatedText = cleanTranslation(translatedText);

        log.info("Gemini API 호출 완료, translatedText={}", translatedText);
        TranslateResponseDto responseDto = new TranslateResponseDto();
        responseDto.setTranslatedText(translatedText);
        return responseDto;
    }

    /**
     * Gemini가 기대하는 구조로 요청 body를 만든다.
     */
    private Map<String, Object> buildPayload(String context) {
        Map<String, Object> part = Map.of("text", context);
        Map<String, Object> content = Map.of("parts", List.of(part));
        return Map.of("contents", List.of(content));
    }

    /**
     * 요청 헤더를 구성. API 키는 헤더 방식으로 전달한다.
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        return headers;
    }

    /**
     * Gemini 응답을 파싱하여 최종 번역 텍스트(가장 위의 후보)를 추출.
     */
    @SuppressWarnings("unchecked")
    private String parseResponse(ResponseEntity<Map> response) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new CustomException(ErrorCode.GM_PARSE_FAILURE);
        }
        Map<String, Object> body = response.getBody();
        Optional<String> extracted = extractTranslatedText(body);
        return extracted.orElseThrow(() -> {
            throw new CustomException(ErrorCode.GM_PARSE_FAILURE);
        });
    }

    /**
     * 가능한 응답 구조들 중에서 가장 상단의 번역 결과(우선순위: 리스트 첫 항목 강조된 문장 > 강조된 문장 > 첫 문장)를 추출.
     */
    private Optional<String> extractTranslatedText(Map<String, Object> body) {
        // 1. candidates -> content.parts[0].text
        if (body.get("candidates") instanceof List<?> candidatesList && !candidatesList.isEmpty()) {
            Object firstCandidate = candidatesList.get(0);
            if (firstCandidate instanceof Map<?, ?> candMap) {
                // nested content.parts[0].text
                Object contentObj = candMap.get("content");
                if (contentObj instanceof Map<?, ?> contentMap) {
                    Object partsObj = contentMap.get("parts");
                    if (partsObj instanceof List<?> partsList && !partsList.isEmpty()) {
                        Object firstPart = partsList.get(0);
                        if (firstPart instanceof Map<?, ?> partMap) {
                            Object textObj = partMap.get("text");
                            if (textObj instanceof String rawText) {
                                return Optional.of(extractTopChoice(rawText));
                            }
                        }
                    }
                }

                // fallback: candidates[0].output
                Object outputObj = candMap.get("output");
                if (outputObj instanceof String out) {
                    return Optional.of(out);
                }
            }
        }

        // 2. outputs[0].content
        if (body.get("outputs") instanceof List<?> outputsList && !outputsList.isEmpty()) {
            Object first = outputsList.get(0);
            if (first instanceof Map<?, ?> outMap) {
                Object content = outMap.get("content");
                if (content instanceof String s) {
                    return Optional.of(s);
                }
            }
        }

        // 3. choices[0].text
        if (body.get("choices") instanceof List<?> choicesList && !choicesList.isEmpty()) {
            Object first = choicesList.get(0);
            if (first instanceof Map<?, ?> choiceMap) {
                Object text = choiceMap.get("text");
                if (text instanceof String s) {
                    return Optional.of(s);
                }
            }
        }
        throw new  CustomException(ErrorCode.GM_UNKNOWN);
    }

    /**
     * rawText에서 가장 위에 있는 번역 후보만 골라낸다.
     * 우선순위:
     * 1) 첫 번째 리스트 항목의 **강조된** 문장
     * 2) 첫 번째 리스트 항목 전체 (마크다운 제거)
     * 3) 전체에서 강조된 문장
     * 4) 첫 문장
     */
    private String extractTopChoice(String rawText) {
        if (rawText == null || rawText.isBlank()) return "";

        // 1. 리스트 항목 줄 탐색 ('*' 또는 '-'로 시작)
        String[] lines = rawText.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("*") || trimmed.startsWith("-")) {
                // 강조된 부분 우선
                Matcher boldMatcher = Pattern.compile("\\*\\*([^*]+)\\*\\*").matcher(trimmed);
                if (boldMatcher.find()) {
                    return boldMatcher.group(1).trim();
                }
                // 강조 없으면 리스트 텍스트 정제
                String cleaned = trimmed.replaceAll("^\\*+\\s*", "")
                        .replace("\\*\\*", "")
                        .trim();
                int parenIdx = cleaned.indexOf(" (");
                if (parenIdx != -1) cleaned = cleaned.substring(0, parenIdx).trim();
                return cleaned;
            }
        }

        // 2. 전체 텍스트에서 강조된 부분
        Matcher boldOnly = Pattern.compile("\\*\\*([^*]+)\\*\\*").matcher(rawText);
        if (boldOnly.find()) {
            return boldOnly.group(1).trim();
        }

        // 3. 첫 문장 (마침표 기준)
        String[] sentences = rawText.split("\\.\\s+");
        if (sentences.length > 0) {
            String first = sentences[0].trim();
            if (!first.endsWith(".")) first += ".";
            return first;
        }

        // fallback
        return rawText.trim();
    }

    /**
     * Gemini 번역 결과에서:
     * 1. 괄호와 그 안의 내용 제거 (예: "(annyeonghaseyo)")
     * 2. 대시 이후 설명 제거 (예: " - ..." 또는 "— ...")
     * 3. 중복 공백 정리, 끝의 마침표/줄임표 정리
     */
    private String cleanTranslation(String raw) {
        if (raw == null) return "";

        // 1. 괄호 안 내용 제거
        String s = raw.replaceAll("\\s*\\([^)]*\\)", "");

        // 2. 대시 이후 설명 제거
        s = s.split("\\s+[-–—]\\s+")[0];

        // 3. 줄임표나 끝에 있는 마침표 제거 (필요하면)
        s = s.replaceAll("[.。…]+$", "").trim();

        // 4. 여러 공백 하나로
        s = s.replaceAll("\\s{2,}", " ");

        return s;
    }
}