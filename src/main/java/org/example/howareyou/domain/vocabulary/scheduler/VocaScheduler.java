package org.example.howareyou.domain.vocabulary.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.vocabulary.service.VocaBookService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class VocaScheduler {

    private final VocaBookService vocaBookService;

    @Scheduled(cron = "0 0 * * * *") // 매 정각 실행
    public void generateVocabularyBookHourly() {
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);

        log.info("[VocaScheduler] 단어장 생성 시작 - {} ~ {}", oneHourAgo, now);

        vocaBookService.generateVocabularyForLastHour(oneHourAgo, now);
    }
}