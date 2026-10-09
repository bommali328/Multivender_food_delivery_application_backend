package com.foodie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // ✅ ఇది ఇంపోర్ట్ చేయండి

@SpringBootApplication
@EnableScheduling // ✅ ఈ అనోటేషన్ ఇక్కడ యాడ్ చేయాలి
public class FoodieApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodieApplication.class, args);
    }
}