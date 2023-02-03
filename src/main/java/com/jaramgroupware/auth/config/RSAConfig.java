package com.jaramgroupware.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 *  RSA 암호화에 사용할 private key와 public key를 load하는 클래스
 *  ref: https://stackoverflow.com/questions/11410770/load-rsa-public-key-from-file
 *  @since 2023-02-03
 *  @author 황준서(37기) hzser123@gmail.com
 */
@Configuration
public class RSAConfig {

    @Bean
    public PrivateKey privateKey() throws Exception {

        byte[] keyBytes = new ClassPathResource("private_key.der").getInputStream().readAllBytes();

        PKCS8EncodedKeySpec spec =
                new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    @Bean
    public PublicKey publicKey() throws Exception {

        byte[] keyBytes =new ClassPathResource("public_key.der").getInputStream().readAllBytes();

        X509EncodedKeySpec spec =
                new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
}
