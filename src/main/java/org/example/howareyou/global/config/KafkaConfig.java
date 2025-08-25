package org.example.howareyou.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Profile("dev")
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;
    private final Environment environment;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        
        // 기본 직렬화 설정
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        
        // 환경별 추가 설정
        String activeProfile = environment.getActiveProfiles()[0];
        log.info("Kafka Producer Factory 생성 - 프로필: {}", activeProfile);
        
        if ("prod".equals(activeProfile)) {
            // AWS MSK 프로덕션 환경 설정
            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.RETRIES_CONFIG, 3);
            props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
            props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
            props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
            log.info("AWS MSK 프로덕션 설정 적용");
        } else {
            // 로컬 개발 환경 설정
            props.put(ProducerConfig.ACKS_CONFIG, "1");
            props.put(ProducerConfig.RETRIES_CONFIG, 1);
            log.info("로컬 개발 환경 설정 적용");
        }
        
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        
        // 프로덕션 환경에서만 에러 핸들러 추가
        String activeProfile = environment.getActiveProfiles()[0];
        if ("prod".equals(activeProfile)) {
            template.setDefaultTopic("default-topic");
            log.info("프로덕션용 KafkaTemplate 설정 완료");
        } else {
            log.info("개발용 KafkaTemplate 설정 완료");
        }
        
        return template;
    }
}
