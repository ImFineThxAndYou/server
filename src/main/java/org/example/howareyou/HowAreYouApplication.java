package org.example.howareyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class HowAreYouApplication {

    public static void main(String[] args) {
        SpringApplication.run(HowAreYouApplication.class, args);
    }

}
