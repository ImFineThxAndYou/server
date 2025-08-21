package org.example.howareyou.global.test;

import java.util.concurrent.CountDownLatch;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@Getter
@Slf4j
public class TestConsumer {

    private CountDownLatch latch = new CountDownLatch(1);
    private String payload;

//    @KafkaListener(topics = "test-topic", groupId = "test-group")
//    public void receive(String message) {
//        log.info("received message='{}'", message);
//        payload = message;
//        latch.countDown();
//    }

    public void reset() {
        latch = new CountDownLatch(1);
    }
}