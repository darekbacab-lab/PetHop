package com.pethop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PetHopApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetHopApplication.class, args);
    }
}
