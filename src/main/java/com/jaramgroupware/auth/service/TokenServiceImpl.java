package com.jaramgroupware.auth.service;

import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenRequestServiceDto;
import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenResponseServiceDto;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthErrorCode;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthException;
import com.jaramgroupware.auth.firebase.FireBaseTokenInfo;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${jwt-key}")
    private String jwtSecret;

    @Value("${jwt-refreshToken.expired}")
    private Integer refreshTokenExpiredDay;

    @Value("${jwt-accessToken.expired}")
    private Integer accessTokenExpiredHour;

    /**
     * 주어진 firebase id token을 남은 유효시간 만큼 block 시키는 함수
     * @param fireBaseTokenInfo block할 firebase token의 정보
     * @return 성공적으로 block 됬는지 여부
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
     */
    @Override
    @Transactional
    public PublishTokenResponseServiceDto publishToken(PublishTokenRequestServiceDto publishTokenRequestDto) {

        PublishTokenResponseServiceDto newTokens = createToken(publishTokenRequestDto.getUserUID(), publishTokenRequestDto.getEmail(), publishTokenRequestDto.getRoleID(),false);

        //오직 refresh 토큰만 유효시간 만큼 캐싱하여 저장함.
        String key = "refresh_"+newTokens.getAccessToken();

        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        valueOperations.set(key,publishTokenRequestDto.getUserUID());
        stringRedisTemplate.expire(key,Duration.ofSeconds(Duration.ofHours(refreshTokenExpiredDay).toSeconds()));

        return newTokens;
    }

    //TODO 토큰발행 및 검증을 종합적으로 관리하는 클래스로 분리하기 (아래 코드는 임시임)
    @Override
    public PublishTokenResponseServiceDto publishAccessToken(PublishTokenRequestServiceDto publishTokenRequestDto) {

        return createToken(publishTokenRequestDto.getUserUID(), publishTokenRequestDto.getEmail(), publishTokenRequestDto.getRoleID(),true);
    }

    /**
     * 해당 access token을 block하는 함수
     * @param accessToken block할 access token
     * @return 성공했다면 true 리턴
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
     */
    @Override
    public boolean checkAccessToken(String accessToken) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        String key = "block_access_token_" + accessToken;
        String result = valueOperations.get(key);
        return result == null;
    }

    /**
     * 해당 refresh token이 유효한지 확인하는 함수
     * @param refreshToken 확인할 refreshToken
     * @return 해당 refresh token 주인의 uid, 유효하지 않다면 null 반환
     */
    @Override
    public String checkRefreshToken(String refreshToken) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        String key = "refresh_" + refreshToken;
        String result = valueOperations.get(key);

        if(result == null) throw new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"주어진 refresh 토큰이 유효하지 않습니다. 다시 로그인해주세요.");
        return result;
    }

    /**
     * 토큰들을 발급하는 클래스
     * 각 토큰의 만료 시간과 토큰에 사용할 시크릿키는 properties 를 통해 관리함
     * jwt-key : 토큰에 사용할 시크릿키
     *
     * jwt-refreshToken.expired : refresh 토큰의 유효시간 (일 단위. ex) 14 -> 14일)
     * jwt-accessToken.expired : access 토큰의 유효시간 (시간 단위. ex) 10 -> 10시간)
     * ref : https://shinsunyoung.tistory.com/110
     *
     * @param uid 발급할 유저의 uid
     * @param email 발급할 유저의 이메일
     * @param role 발급할 유저의 role id
     * @param onlyAccessToken access token만 발급할지 여부, 만약 false 일 경우 access token과 refresh token을 동시에 발급함.
     * @return PublishTokenResponseServiceDto로 토큰 발급 결과를 리턴, onlyAccessToken이 true일 경우, refreshToken,refreshTokenExpired 은 null로 리턴함.
     */
    private PublishTokenResponseServiceDto createToken(String uid,String email,Integer role,boolean onlyAccessToken){
        Date now = new Date();
        Date accessExpired = new Date(now.getTime() + Duration.ofHours(accessTokenExpiredHour).toMillis());
        String refreshToken = null;
        Date refreshExpired = null;

        //TODO 토큰발행 및 검증을 종합적으로 관리하는 클래스로 분리하기
        String accessToken = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer("jaram_access")
                .setIssuedAt(now)
                .setExpiration(accessExpired)
                .claim("uid", uid)
                .claim("email", email)
                .claim("role",role)
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();


        if(!onlyAccessToken){
            refreshExpired = new Date(now.getTime() + Duration.ofDays(refreshTokenExpiredDay).toMillis());
            refreshToken = Jwts.builder()
                    .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                    .setIssuer("jaram_refresh")
                    .setIssuedAt(now)
                    .setExpiration(refreshExpired)
                    .claim("uid", uid)
                    .claim("email", email)
                    .claim("role",role)
                    .signWith(SignatureAlgorithm.HS256, jwtSecret)
                    .compact();
        }


        return PublishTokenResponseServiceDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .refreshTokenExpired(refreshExpired)
                .accessTokenExpired(accessExpired)
                .build();
    }
}
