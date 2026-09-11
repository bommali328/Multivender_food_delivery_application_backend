package com.foodie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*"); 
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. KYC ఫోల్డర్ మ్యాపింగ్
        Path kycUploadDir = Paths.get("uploads/kyc/");
        String kycUploadPath = kycUploadDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/uploads/kyc/**")
                .addResourceLocations("file:" + kycUploadPath + "/")
                .setCachePeriod(0);

        // 2. జనరల్ షాప్ & ఐటమ్స్ అప్‌లోడ్స్ ఫోల్డర్ మ్యాపింగ్
        Path generalUploadDir = Paths.get("uploads/");
        String generalUploadPath = generalUploadDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + generalUploadPath + "/")
                .setCachePeriod(0);
    }
    
}