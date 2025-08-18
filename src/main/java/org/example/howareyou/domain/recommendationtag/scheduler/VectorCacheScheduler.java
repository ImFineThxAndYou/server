package org.example.howareyou.domain.recommendationtag.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.recommendationtag.service.VectorCacheBatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VectorCacheScheduler {

  private final VectorCacheBatchService batchService;

  // 매일 새벽 2시에 실행
  @Scheduled(cron = "0 0 2 * * *")
  public void runDailyCache() {
    batchService.cacheAllMemberVectors();
  }

}
