package com.foodie.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    @Primary
    public FirebaseMessaging firebaseMessaging() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = FirebaseConfig.class
                        .getClassLoader()
                        .getResourceAsStream("serviceAccountKey.json");

                if (serviceAccount != null) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                    FirebaseApp.initializeApp(options);
                } else {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.getApplicationDefault())
                            .build();
                    FirebaseApp.initializeApp(options);
                }
            }
            return FirebaseMessaging.getInstance();
        } catch (Exception e) {
            System.err.println("Firebase Init Warning (App will run without FCM): " + e.getMessage());
            return null; // ఫైర్‌బేస్ లేకపోయినా యాప్ క్రాష్ అవ్వకుండా రన్ అవుతుంది
        }
    }
}