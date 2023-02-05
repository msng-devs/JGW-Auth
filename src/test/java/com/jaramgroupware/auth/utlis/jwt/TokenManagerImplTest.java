package com.jaramgroupware.auth.utlis.jwt;

import com.jaramgroupware.auth.testConfig.TestJackson2ObjectConfig;
import com.jaramgroupware.auth.testConfig.TestRSAConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

@ComponentScan
@Import({TestRSAConfig.class})
@ExtendWith(MockitoExtension.class)
class TokenManagerImplTest {

    @InjectMocks
    private TokenManagerImpl tokenManager;
    private String testRefreshToken = null;
    private String testAccessToken = null;

    @Value("${jwt-refreshToken.expired}")
    private Integer refreshTokenExpiredSec;

    @Value("${jwt-accessToken.expired}")
    private Integer accessTokenExpiredSec;

    @Autowired
    private PublicKey publicKey;

    @Autowired
    private PrivateKey privateKey;

    @BeforeEach
    void testSetUp(){

    }

    @Test
    void decodeToken() {
    }

    @Test
    void createToken() {
    }

    @Test
    void verifyToken() {
    }

    @Test
    void testVerifyToken() {
    }
}