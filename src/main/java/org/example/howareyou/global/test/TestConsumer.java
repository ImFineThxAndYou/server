package org.example.howareyou.global.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
@Slf4j
public class TestConsumer {

    private String payload;
    private CountDownLatch latch = new CountDownLatch(1);

    @KafkaListener(topics = "test-topic", groupId = "group_1")
    public void listener(Object data) {
        log.info("Received payload='{}'", data);
    }

    public String getPayload() {
        return payload;
    }

    public CountDownLatch getLatch() {
        return latch;
    }
}
