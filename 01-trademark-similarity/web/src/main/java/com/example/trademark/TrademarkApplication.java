package com.example.trademark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TrademarkApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrademarkApplication.class, args);
    }
}
