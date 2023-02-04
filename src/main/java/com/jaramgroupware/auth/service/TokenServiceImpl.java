package com.jaramgroupware.auth.service;

import com.jaramgroupware.auth.dto.token.serviceDto.PublishAccessTokenResponseServiceDto;
import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenRequestServiceDto;
import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenResponseServiceDto;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthErrorCode;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthException;
import com.jaramgroupware.auth.firebase.FireBaseTokenInfo;
import com.jaramgroupware.auth.utlis.jwt.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Redis에 각종 토큰 정보를 저장하는 클래스
 * @since 2023-01-27
 * @author 황준서(37기) hzser123@gmail.com
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TokenServiceImpl implements TokenService {

    private StringRedisTemplate stringRedisTemplate;
    private TokenManagerImpl tokenManager;

    /**
     * 주어진 firebase id token을 남은 유효시간 만큼 block 시키는 함수
     * @param fireBaseTokenInfo block할 firebase token의 정보
     * @return 성공적으로 block 됬는지 여부
     * @author 황준서(37기) hzser123@gmail.com
     */
    @Override
    @Transactional
    public boolean blockFirebaseIdToken(FireBaseTokenInfo fireBaseTokenInfo) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        String key = "firebase_"+fireBaseTokenInfo.getIdToken();
        valueOperations.set(key, fireBaseTokenInfo.getUid());
        stringRedisTemplate.expire(key, Duration.ofSeconds(Duration.between(fireBaseTokenInfo.getExpireDateTime(), LocalDateTime.now()).toSeconds()));
        return true;
    }

    /**
     * 주어진 firebase id token이 block된 토큰인지 확인하는 함수
     * @param fireBaseToken 확인할 firebase token
     * @return 만약 block 되지 않은 토큰일경우 true 리턴
     * @author 황준서(37기) hzser123@gmail.com
     */
    @Override
    public boolean checkFirebaseIdToken(String fireBaseToken) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        String key = "firebase_"+fireBaseToken;
        String result = valueOperations.get(key);

        return result == null;
    }

    /**
     * 신규 refresh token과 신규 access token을 발급하는 함수
     * @param publishTokenRequestDto 토큰의 claim에 작성될 유저의 정보
     * @return PublishTokenResponseServiceDto Access Token & Refresh Token과 두 토큰의 만료일
     * @author 황준서(37기) hzser123@gmail.com
     */
    @Override
    @Transactional
    public PublishTokenResponseServiceDto publishToken(PublishTokenRequestServiceDto publishTokenRequestDto) {
        var now = LocalDateTime.now();

        var jwtCreateTokenInfo = JwtCreateTokenInfo.builder()
                .email(publishTokenRequestDto.getEmail())
                .role(publishTokenRequestDto.getRoleID())
                .uid(publishTokenRequestDto.getUserUID())
                .build();

        JwtCreateTokenResult newAccessTokenInfo = tokenManager.createToken(jwtCreateTokenInfo,true);
        JwtCreateTokenResult newRefreshTokenInfo = tokenManager.createToken(jwtCreateTokenInfo,false);

        //오직 refresh 토큰만 유효시간 만큼 캐싱하여 저장함.
        String key = "refresh_"+newRefreshTokenInfo.getToken();

        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        valueOperations.set(key,publishTokenRequestDto.getUserUID());
        stringRedisTemplate.expire(key,Duration.ofSeconds(
                Duration.between(now,newRefreshTokenInfo.getExpiredAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()).toSeconds()
        ));

        return PublishTokenResponseServiceDto.builder()
                .accessToken(newAccessTokenInfo.getToken())
                .accessTokenExpired(newAccessTokenInfo.getExpiredAt())
                .refreshTokenExpired(newRefreshTokenInfo.getExpiredAt())
                .build();
    }

    /**
     * 신규 accessToken을 발급하는 함수
     * @param publishTokenRequestDto 발급할 AccessToken의 claim에 사용할 정보
     * @return PublishAccessTokenResponseServiceDto 생성된 AccessToken과 만료일을 반환
     * @author 황준서(37기) hzser123@gmail.com
     */
    //TODO Token을 통합적으로 관리하는 클래스가 추가됨에 따라 해당 함수는 삭제하기
    @Override
    public PublishAccessTokenResponseServiceDto publishAccessToken(PublishTokenRequestServiceDto publishTokenRequestDto) {

        JwtCreateTokenResult newAccessTokenInfo = tokenManager.createToken(JwtCreateTokenInfo.builder()
                .email(publishTokenRequestDto.getEmail())
                .role(publishTokenRequestDto.getRoleID())
                .uid(publishTokenRequestDto.getUserUID())
                .build(),true);

        return PublishAccessTokenResponseServiceDto.builder()
                .accessToken(newAccessTokenInfo.getToken())
                .accessTokenExpired(newAccessTokenInfo.getExpiredAt())
                .build();
    }

    /**
     * 해당 access token을 block하는 함수
     * @param accessToken block할 access token
     * @return 성공했다면 true 리턴
     * @author 황준서(37기) hzser123@gmail.com
     */
    @Override
    @Transactional
    public Boolean revokeAccessToken(String accessToken,String uid,Date expiredDate) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        String key = "block_access_token_" + accessToken;
        valueOperations.set(key,uid);
        stringRedisTemplate.expire(key,Duration.ofSeconds(Duration.between(LocalDateTime.now(), expiredDate.toInstant()   // Date -> Instant
                .atZone(ZoneId.systemDefault())
                .toLocalDate()).toSeconds()));
        return true;
    }

    /**
     * 해당 refresh token을 제거하는 함수
     * @param refreshToken 제거할 refresh token
     * @return 성공했다면 true 리턴
     * @author 황준서(37기) hzser123@gmail.com
     */
    @Override
    public Boolean revokeRefreshToken(String refreshToken) {
        String key = "refresh_"+refreshToken;
        return stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Boolean revokeUserToken(String userUID) {
        //TODO userUID로 모든 refresh token 삭제 로직 추가
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        return null;
    }

    /**
     * 해당 access token이 block 됬는지 확인하는 함수
     * @param accessToken 확인할 access token
     * @return true라면 block 되지 않는 토큰임
     * @throws JGWAuthException 만약 block된 token이라면 NOT_VALID_TOKEN 발생
     * @author 황준서(37기) hzser123@gmail.com
     */
    @Override
    public boolean checkAccessToken(String accessToken) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        String key = "block_access_token_" + accessToken;
        String result = valueOperations.get(key);

        if(result != null) throw new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"사용 불가능한 토큰입니다. 다시 로그인해주세요.");

        return true;
    }

    /**
     * 해당 refresh token이 유효한지 확인하는 함수
     * @param refreshToken 확인할 refreshToken
     * @throws JGWAuthException 해당 refresh token의 정보가 없으면 NOT_VALID_TOKEN를 리턴함.
     * @return 해당 refresh token 주인의 uid를 리턴함.
     * @author 황준서(37기) hzser123@gmail.com
     */
    @Override
    public String checkRefreshToken(String refreshToken) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        String key = "refresh_" + refreshToken;
        String result = valueOperations.get(key);

        if(result == null) throw new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"주어진 refresh 토큰이 유효하지 않습니다. 다시 로그인해주세요.");
        return result;
    }

}
