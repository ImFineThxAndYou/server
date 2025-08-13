package org.example.howareyou.domain.vocabulary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.vocabulary.dto.AnalyzeRequestDto;
import org.example.howareyou.domain.vocabulary.dto.AnalyzedResponseWord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class NlpClient {

    private final WebClient nlpWebClient;

    public NlpClient(@Qualifier("nlpWebClient") WebClient webClient) {
        this.nlpWebClient = webClient;
    }

  public List<AnalyzedResponseWord> analyze(String text) {
        try {
            Mono<List<AnalyzedResponseWord>> response = nlpWebClient.post()
                    .uri("/analyze/mixed")
                    .bodyValue(new AnalyzeRequestDto(text))
                    .retrieve()
                    .bodyToFlux(AnalyzedResponseWord.class)
                    .collectList();

            return response.block(); // blocking 처리
        } catch (Exception e) {
            log.error("NLP 분석 실패: {}", e.getMessage());
            return List.of(); // 빈 결과 반환
        }
    }
}
