package com.foodie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; 
import org.springframework.scheduling.annotation.EnableAsync; // ✅ ఇది మిస్సయింది, ఇక్కడ ఇంపోర్ట్ చేయండి

@SpringBootApplication
@EnableScheduling 
@EnableAsync // ✅ అసింక్రోనస్ (Async) కోసం ఇది ఎనేబుల్ చేయబడింది
public class FoodieApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodieApplication.class, args);
    }
}