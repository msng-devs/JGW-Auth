package com.jaramgroupware.jgwauth.testConfig;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@TestConfiguration
public class TestFireBaseConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        return null;
    }


    @Bean
    public FirebaseAuth getFirebaseAuth() throws IOException {
        return null;
    }
}
