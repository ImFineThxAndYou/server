package org.example.howareyou.global;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.example.howareyou.global.test.TestConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, topics = { "test-topic" })
@DirtiesContext
class KafkaTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private TestConsumer consumer;

    @Test
    void consumer_should_receive_message() throws Exception {
        // given
        String topic = "test-topic";
        String message = "test-message";

        // when
        kafkaTemplate.send(topic, message);

        // then
        boolean messageConsumed = consumer.getLatch().await(10, TimeUnit.SECONDS);
        assertThat(messageConsumed).isTrue();
        assertThat(consumer.getPayload()).isEqualTo(message);
    }
}
