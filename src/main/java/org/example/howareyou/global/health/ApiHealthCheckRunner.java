package org.example.howareyou.global.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Profile("dev")
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApiHealthCheckRunner implements ApplicationRunner {

    private final WebClient nlpWebClient;
    private final WebClient taggingNlpWebClient;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Performing health checks for external APIs. The application will not start if any API is down.");

        Mono<String> spacyApiHealth = nlpWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofSeconds(5));

        Mono<String> fastApiHealth = taggingNlpWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofSeconds(5));

        try {
            Mono.zip(spacyApiHealth, fastApiHealth)
                    .doOnSuccess(results -> {
                        log.info("Spacy API (port 8000) health check successful: {}", results.getT1());
                        log.info("FastAPI (port 8001) health check successful: {}", results.getT2());
                        log.info("All external APIs are healthy. Proceeding with application startup.");
                    })
                    .block(); // Block and wait for the result, will throw exception on error
        } catch (Exception e) {
            log.error("External API health check failed. Application startup will be aborted.", e);
            // Re-throw the exception to stop the application context from loading
            throw new RuntimeException("External API health check failed", e);
        }
    }
}
