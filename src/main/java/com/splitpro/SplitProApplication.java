package com.splitpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class SplitProApplication {
    public static void main(String[] args) {
        SpringApplication.run(SplitProApplication.class, args);
    }
}