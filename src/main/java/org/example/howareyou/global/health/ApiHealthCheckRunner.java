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
    private final WebClient translateWebClient;
    private final WebClient geminiWebClient;

    @Override
    public void run(ApplicationArguments args) {
        log.info("🔍 Performing health checks for external APIs. The application will not start if any API is down.");

        // Define each API check
        Mono<String> spacyApiHealth = checkHealth(nlpWebClient, "/health", "Spacy API (port 8000)");
        Mono<String> fastApiHealth = checkHealth(taggingNlpWebClient, "/health", "FastAPI (port 8001)");
        Mono<String> libreTranslateHealth = checkHealth(translateWebClient, "/languages", "LibreTranslate API");
        Mono<String> geminiApiHealth = checkHealth(geminiWebClient, "", "Gemini API");

        try {
            Mono.zip(spacyApiHealth, fastApiHealth, libreTranslateHealth, geminiApiHealth)
                    .doOnSuccess(results -> {
                        log.info("✅ All external APIs are healthy. Proceeding with application startup.");
                    })
                    .block(); // block until all health checks complete
        } catch (Exception e) {
            log.error("❌ One or more external API health checks failed. Aborting application startup.", e);
            throw new RuntimeException("External API health check failed", e);
        }
    }

    private Mono<String> checkHealth(WebClient client, String uri, String apiName) {
        return client.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofSeconds(10))
                .doOnSuccess(res -> log.info("✅ {} health check successful: {}", apiName, res))
                .doOnError(err -> log.error("❌ {} health check failed: {}", apiName, err.getMessage()));
    }
}