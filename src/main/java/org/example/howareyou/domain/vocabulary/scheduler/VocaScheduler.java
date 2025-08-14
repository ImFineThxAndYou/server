package org.example.howareyou.domain.vocabulary.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.vocabulary.service.ChatVocaBookService;
import org.example.howareyou.domain.vocabulary.service.MemberVocaBookService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class VocaScheduler {

    private final ChatVocaBookService chatVocaBookService;
    private final MemberVocaBookService memberVocaBookService;

    @Scheduled(cron = "0 0 * * * *") // 매 정각 실행
    public void generateVocabularyBookHourly() {
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);

        log.info("채팅방 별 단어장 생성 시작 - {} ~ {}", oneHourAgo, now);

        chatVocaBookService.generateVocabularyForRangeReactive(oneHourAgo, now)
                .subscribe(
                        v -> {},
                        ex -> log.error("배치 실패", ex),
                        () -> log.info("배치 완료: {} ~ {}", oneHourAgo, now)
                );
    }


    // 사용자 단어장: 매 정각 (UTC)
    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void driveMemberTimezoneBatch() {
        memberVocaBookService.runByTimezoneWindow();
    }
}