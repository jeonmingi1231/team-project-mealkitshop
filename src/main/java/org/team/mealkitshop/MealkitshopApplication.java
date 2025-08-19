package org.team.mealkitshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MealkitshopApplication {

    public static void main(String[] args) {

        SpringApplication.run(MealkitshopApplication.class, args);

    }

}