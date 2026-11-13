package com.example.stress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StressApplication {

    public static void main(String[] args) {
        SpringApplication.run(StressApplication.class, args);
    }
}
