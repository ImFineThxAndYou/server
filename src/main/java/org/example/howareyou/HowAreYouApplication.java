package org.example.howareyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableKafka
@EnableJpaAuditing
@SpringBootApplication
@EnableScheduling
public class HowAreYouApplication {

    public static void main(String[] args) {
        SpringApplication.run(HowAreYouApplication.class, args);
    }

}
