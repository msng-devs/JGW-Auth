package com.jaramgroupware.auth.service;

import com.jaramgroupware.auth.firebase.FireBaseTokenInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ComponentScan
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @InjectMocks
    private TokenServiceImpl tokenService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private final ValueOperations valueOperations = Mockito.mock(ValueOperations.class);

    @Test
    void blockFirebaseIdToken() {
        //given
        FireBaseTokenInfo testInfo = FireBaseTokenInfo.builder()
                .uid("test")
                .expireDateTime(LocalDateTime.MAX)
                .idToken("testidtoken")
                .build();

        doReturn(valueOperations).when(stringRedisTemplate).opsForValue();

        //when
        boolean result = tokenService.blockFirebaseIdToken(testInfo);

        //then
        verify(stringRedisTemplate).opsForValue();
        verify(valueOperations).set("firebase_"+testInfo.getIdToken(),testInfo.getUid());
        verify(stringRedisTemplate).expire("firebase_"+testInfo.getIdToken(), Duration.ofSeconds(Duration.between(testInfo.getExpireDateTime(), LocalDateTime.now()).toSeconds()));
        assertTrue(result);
    }

    @Test
    void checkFirebaseIdToken() {
    }

    @Test
    void publishToken() {
    }

    @Test
    void revokeAccessToken() {
    }

    @Test
    void revokeRefreshToken() {
    }

    @Test
    void revokeUserToken() {
    }

    @Test
    void checkAccessToken() {
    }

    @Test
    void checkRefreshToken() {
    }
}