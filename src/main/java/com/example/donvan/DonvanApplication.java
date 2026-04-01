package com.example.donvan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DonvanApplication {

    public static void main(String[] args) {
        SpringApplication.run(DonvanApplication.class, args);
    }

}
