package org.example.howareyou.domain.recommendationtag.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.recommendationtag.dto.ClassifyRequest;
import org.example.howareyou.domain.recommendationtag.dto.ClassifyResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TaggingNlpClient {

  private final WebClient webClient;

  public TaggingNlpClient(@Qualifier("taggingNlpWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  public Map<String, Double> classifyWords(List<String> words) {
    try {
      return webClient.post()
          .uri("/classify")
          .bodyValue(new ClassifyRequest(words))
          .retrieve()
          .bodyToMono(ClassifyResponse.class)
          .timeout(Duration.ofSeconds(3))
          .map(ClassifyResponse::getScores)
          .block();
    } catch (Exception e) {
      log.error("Tagging classifyWords 실패: {}", e.getMessage());
      return Map.of();
    }
  }

  public Map<String, Double> classifyMember(long memberId) {
    try {
      return webClient.get()
          .uri("/members/{id}/classify", memberId)
          .retrieve()
          .bodyToMono(ClassifyResponse.class)
          .timeout(Duration.ofSeconds(5))
          .map(ClassifyResponse::getScores)
          .block();
    } catch (Exception e) {
      log.error("Tagging classifyMember 실패: {}", e.getMessage());
      return Map.of();
    }
  }
}