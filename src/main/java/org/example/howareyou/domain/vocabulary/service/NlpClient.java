package org.example.howareyou.domain.vocabulary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.vocabulary.dto.AnalyzeBatchRequest;
import org.example.howareyou.domain.vocabulary.dto.AnalyzeRequestDto;
import org.example.howareyou.domain.vocabulary.dto.AnalyzedResponseWord;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NlpClient {

    private final WebClient nlpWebClient;

    /** 단일 텍스트 분석*/
    public Mono<List<AnalyzedResponseWord>> analyzeReactive(String text) {
        return nlpWebClient.post()
                .uri("/analyze/mixed")
                .bodyValue(new AnalyzeRequestDto(text))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.warn("NLP 서버 에러: status={}, body={}", resp.statusCode(), body);
                                    return Mono.error(new RuntimeException("NLP error: " + resp.statusCode()));
                                })
                )
                .bodyToFlux(AnalyzedResponseWord.class)
                .collectList()
                .timeout(Duration.ofSeconds(5))
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(200))
                                .filter(ex -> true) // 필요시 5xx만 재시도 등으로 좁히세요
                )
                .doOnError(e -> log.error("NLP 분석 실패", e));
    }

    /** ✅ 배치 분석: 메시지 리스트(JSON) 통째로 전달 */
    public Mono<List<AnalyzedResponseWord>> analyzeBatchReactive(AnalyzeBatchRequest request) {
        return nlpWebClient.post()
                .uri("/analyze/mixed-batch")
                .bodyValue(request)  // { chatRoomUuid, messages: [{messageId, content, ...}, ...] }
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.warn("NLP 서버 에러(batch): status={}, body={}", resp.statusCode(), body);
                                    return Mono.error(new RuntimeException("NLP batch error: " + resp.statusCode()));
                                })
                )
                .bodyToFlux(AnalyzedResponseWord.class)
                .collectList()
                .timeout(Duration.ofSeconds(10))            // 배치는 여유를 조금 더 줌
                .retryWhen(Retry.backoff(2, Duration.ofMillis(300)))
                .doOnSubscribe(s -> log.info("▶️ NLP 배치 분석 시작 - room={}, size={}",
                        request.chatRoomUuid(), request.messages().size()))
                .doOnNext(list -> log.info("✅ NLP 배치 분석 완료 - tokens={}", list.size()))
                .doOnError(e -> log.error("NLP 배치 분석 실패", e));
    }


}
