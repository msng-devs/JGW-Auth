package com.jaramgroupware.jgwauth.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FireBaseConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        logger.info("Initializing Firebase.");
        FileInputStream serviceAccount =
                new FileInputStream("src/main/resources/firebase.json");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket("heroku-sample.appspot.com")
                .build();
        FirebaseApp app;
        if(FirebaseApp.getApps().size() == 0){
            app = FirebaseApp.initializeApp(options);
        }else{
            app = FirebaseApp.getApps().get(0);
        }

        logger.info("FirebaseApp initialized" + app.getName());

        return app;
    }


    @Bean
    public FirebaseAuth getFirebaseAuth() throws IOException {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance(firebaseApp());
        return firebaseAuth;
    }
}
