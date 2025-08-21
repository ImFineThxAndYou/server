package org.example.howareyou.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        JavaTimeModule module = new JavaTimeModule();
        
        // Configure Instant serialization to use ISO-8601 format in UTC
        module.addSerializer(Instant.class, InstantSerializer.INSTANCE);
        
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .modules(module)
                .build();
        
        // CustomMemberDetails 역직렬화 차단 (전역 설정)
        mapper.addMixIn(CustomMemberDetails.class, CustomMemberDetailsMixin.class);
        
        return mapper;
    }
    
    // CustomMemberDetails 역직렬화 방지를 위한 Mixin
    @com.fasterxml.jackson.annotation.JsonIgnoreType
    private static abstract class CustomMemberDetailsMixin {
        // 이 클래스 타입 자체를 Jackson에서 완전히 무시
    }
}
