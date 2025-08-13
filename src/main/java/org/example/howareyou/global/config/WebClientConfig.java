package org.example.howareyou.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // NLP 분석용 WebClient
    @Bean(name = "nlpWebClient") // 8000용 (기존)
    public WebClient nlpWebClient(@Value("${nlp.base-url}") String baseUrl,
        WebClient.Builder builder) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean(name = "taggingNlpWebClient") // 8001용 (신규)
    public WebClient taggingNlpWebClient(@Value("${tagging-nlp.base-url}") String baseUrl,
        WebClient.Builder builder) {
        return builder.baseUrl(baseUrl).build();
    }
}
