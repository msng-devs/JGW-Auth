package com.jaramgroupware.auth.service;

import com.jaramgroupware.auth.dto.token.serviceDto.PublishAccessTokenResponseServiceDto;
import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenRequestServiceDto;
import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenResponseServiceDto;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthException;
import com.jaramgroupware.auth.firebase.FireBaseTokenInfo;
import com.jaramgroupware.auth.testUtils.TestUtils;
import com.jaramgroupware.auth.utlis.jwt.JwtCreateTokenInfo;
import com.jaramgroupware.auth.utlis.jwt.JwtCreateTokenResult;
import com.jaramgroupware.auth.utlis.jwt.TokenManagerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Description;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

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
    private TokenManagerImpl tokenManager;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private final TestUtils testUtils = new TestUtils();

    private final ValueOperations valueOperations = Mockito.mock(ValueOperations.class);

    @Test
    @Description("blockFirebaseIdToken - idToken의 정보가 주어지면, 해당 id token을 유효시간 만큼 등록한다.")
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
    @Description("checkFirebaseIdToken - block 되지 않은 id token이 주어지면, true를 반환한다.")
    void checkFirebaseIdToken() {
        //given
        String testIdToken = "testidtoken";

        doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
        doReturn(null).when(valueOperations).get( "firebase_"+testIdToken);
        //when
        boolean result = tokenService.checkFirebaseIdToken(testIdToken);

        //then
        verify(stringRedisTemplate).opsForValue();
        verify(valueOperations).get("firebase_"+testIdToken);
        assertTrue(result);
    }

    @Test
    @Description("checkFirebaseIdToken2 - block된 id token이 주어지면, false를 반환한다.")
    void checkFirebaseIdToken2() {
        //given
        String testIdToken = "testidtoken";

        doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
        doReturn("thisisUserUid").when(valueOperations).get( "firebase_"+testIdToken);
        //when
        boolean result = tokenService.checkFirebaseIdToken(testIdToken);

        //then
        verify(stringRedisTemplate).opsForValue();
        verify(valueOperations).get("firebase_"+testIdToken);
        assertFalse(result);
    }

    @Test
    @Description("publishToken - 토큰을 발급할 정보를 입력받으면, refresh token, access token의 정보를 리턴하고, refresh token을 저장한다.")
    void publishToken() {

        //given
        var testExpiredAt = new Date(new Date().getTime() + Duration.ofSeconds(100).toMillis());
        var testMember = testUtils.getTestMember();
        var jwtCreateTokenInfo = JwtCreateTokenInfo.builder()
                .email(testMember.getEmail())
                .role(testMember.getRole())
                .uid(testMember.getId())
                .build();

        var testAccessToken = "thisIsTestAccessToken";
        var testRefreshToken = "thisIsTestRefreshToken";

        var testRequest = PublishTokenRequestServiceDto
                .builder()
                .email(testMember.getEmail())
                .roleID(testMember.getRole())
                .userUID(testMember.getId())
                .build();

        var testExceptedResult = PublishTokenResponseServiceDto.builder()
                .accessToken(testAccessToken)
                .accessTokenExpired(testExpiredAt)
                .refreshTokenExpired(testExpiredAt)
                .build();

        doReturn(JwtCreateTokenResult.builder().token(testAccessToken).expiredAt(testExpiredAt).build()).when(tokenManager).createToken(jwtCreateTokenInfo,true);
        doReturn(JwtCreateTokenResult.builder().token(testRefreshToken).expiredAt(testExpiredAt).build()).when(tokenManager).createToken(jwtCreateTokenInfo,false);
        doReturn(valueOperations).when(stringRedisTemplate).opsForValue();

        //when
        var testResult = tokenService.publishToken(testRequest);

        //then
        assertEquals(testExceptedResult,testResult);
        verify(tokenManager).createToken(jwtCreateTokenInfo,true);
        verify(tokenManager).createToken(jwtCreateTokenInfo,false);
        verify(stringRedisTemplate).opsForValue();
        verify(valueOperations).set("refresh_"+testRefreshToken,testMember.getId());
        verify(stringRedisTemplate).expire("refresh_"+testRefreshToken,Duration.ofSeconds(100));

    }
    @Test
    @Description("publishAccessToken - 생성할 토큰의 정보가 주어지면, 새로운 accessToken을 발급한다.")
    void publishAccessToken(){
        //given
        var testAccessToken = "thisIsTestAccessToken";
        var testMember = testUtils.getTestMember();
        var testExpiredAt = new Date();
        var testRequest = PublishTokenRequestServiceDto
                .builder()
                .email(testMember.getEmail())
                .roleID(testMember.getRole())
                .userUID(testMember.getId())
                .build();

        var jwtCreateTokenInfo = JwtCreateTokenInfo.builder()
                .email(testMember.getEmail())
                .role(testMember.getRole())
                .uid(testMember.getId())
                .build();

        var testExceptedResult = PublishAccessTokenResponseServiceDto
                .builder()
                .accessToken(testAccessToken)
                .accessTokenExpired(testExpiredAt)
                .build();

        doReturn(JwtCreateTokenResult.builder().token(testAccessToken).expiredAt(testExpiredAt).build()).when(tokenManager).createToken(jwtCreateTokenInfo,true);

        //when
        var testResult = tokenService.publishAccessToken(testRequest);
        //then
        assertEquals(testExceptedResult,testResult);
        verify(tokenManager).createToken(jwtCreateTokenInfo,true);
    }

    @Test
    @Description("revokeAccessToken - access token이 주어지면, 주어진 시간 만큼 등록한다.")
    void revokeAccessToken() {
        //given
        var testAccessToken = "thisIsTestAccessToken!";
        var testUid = testUtils.getTestUid();
        var testExpiredAt = new Date(new Date().getTime() + Duration.ofSeconds(100).toMillis());

        doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
        //when
        var testResult = tokenService.revokeAccessToken(testAccessToken,testUid,testExpiredAt);
        //then
        assertTrue(testResult);
        verify(valueOperations).set("block_access_token_"+testAccessToken,testUid);
        verify(stringRedisTemplate).expire("block_access_token_"+testAccessToken,Duration.ofSeconds(100));
    }

    @Test
    @Description("revokeRefreshToken - refresh token이 주어지면, 해당 토큰을 삭제한다.")
    void revokeRefreshToken() {
        //given
        String testRefreshToken = "thisIsTestRefreshToken";
        doReturn(true).when(stringRedisTemplate).delete("refresh_"+testRefreshToken);
        //when
        var testResult = tokenService.revokeRefreshToken(testRefreshToken);
        //then
        assertTrue(testResult);
        verify(stringRedisTemplate).delete("refresh_"+testRefreshToken);
    }

    @Test
    void revokeUserToken() {

    }

    @Test
    @Description("checkAccessToken - block 되지 않은 access token이 주어지면, true를 리턴한다.")
    void checkAccessToken() {
        //given
        var testAccessToken = "thisIsTestAccessToken!";

        doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
        doReturn(null).when(valueOperations).get("block_access_token_"+testAccessToken);
        //when
        var testResult = tokenService.checkAccessToken(testAccessToken);

        //then
        assertTrue(testResult);
        verify(stringRedisTemplate).opsForValue();
        verify(valueOperations).get("block_access_token_"+testAccessToken);

    }

    @Test
    @Description("checkAccessToken - block된 않은 access token이 주어지면, JGWAuthException이 발생한다.")
    void checkAccessToken2() {
        //given
        var testAccessToken = "thisIsTestAccessToken!";

        doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
        doReturn(testUtils.getTestUid()).when(valueOperations).get("block_access_token_"+testAccessToken);

        //when
        assertThrows(JGWAuthException.class,() -> tokenService.checkAccessToken(testAccessToken));


    }

    @Test
    @Description("checkRefreshToken - 등록된 refresh token이 주어지면, 해당 유저의 uid를 리턴한다.")
    void checkRefreshToken() {
        //given
        var testRefresh = "thisIsTestRefreshToken!";

        doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
        doReturn(testUtils.getTestUid()).when(valueOperations).get("refresh_"+testRefresh);
        //when
        var testResult = tokenService.checkRefreshToken(testRefresh);

        //then
        assertEquals(testUtils.getTestUid(),testResult);
        verify(stringRedisTemplate).opsForValue();
        verify(valueOperations).get("refresh_"+testRefresh);
    }

    @Test
    @Description("checkRefreshToken - 등록된 refresh token이 주어지면, JGWAuthException이 발생한다.")
    void checkRefreshToken2() {
        //given
        var testRefresh = "thisIsTestRefreshToken!";

        doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
        doReturn(null).when(valueOperations).get("refresh_"+testRefresh);

        //when
        assertThrows(JGWAuthException.class,() -> tokenService.checkRefreshToken(testRefresh));

    }
}