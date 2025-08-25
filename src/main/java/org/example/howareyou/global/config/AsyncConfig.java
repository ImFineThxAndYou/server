// config/AsyncConfig.java
package org.example.howareyou.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "notificationExecutor")
  public Executor notificationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);      // 동시에 처리할 최소 스레드 수
    executor.setMaxPoolSize(20);      // 최대 스레드 수
    executor.setQueueCapacity(1000);  // 큐에 쌓일 수 있는 작업 수
    executor.setThreadNamePrefix("notify-");
    executor.initialize();
    return executor;
  }
}
