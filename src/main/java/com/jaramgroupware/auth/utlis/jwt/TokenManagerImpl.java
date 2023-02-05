package com.jaramgroupware.auth.utlis.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthErrorCode;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Date;

/**
 * 토큰을 발급 및 검증을 수행하는 class
 * @since 2023-01-27
 * @author 황준서(37기) hzser123@gmail.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenManagerImpl implements TokenManager {

    @Qualifier("RSAPublicKey")
    private final PublicKey publicKey;

    @Qualifier("RSAPrivateKey")
    private final PrivateKey privateKey;

    @Value("${jwt-refreshToken.expired}")
    private Integer refreshTokenExpiredSec;

    @Value("${jwt-accessToken.expired}")
    private Integer accessTokenExpiredSec;

    private Algorithm algorithm = null;
    private JWTVerifier verifier = null;

    @PostConstruct
    private void setUp(){
        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;
        algorithm = Algorithm.RSA256(rsaPublicKey, rsaPrivateKey);
        verifier = JWT.require(algorithm)
                .withIssuer("jaramgroupware")
                .build();
    }

    /**
     * 입력받은 토큰을 decode하고, 기본적인 검증(alg 검증, 유효시간 검증, sub 검증, claim 검증)을 수행한다.
     * @param token 해제할 토큰
     * @throws JGWAuthException 해당 토큰이 유효하지 않다면 NOT_VALID_TOKEN 발생
     * @return JwtTokenInfo 해당 토큰의 정보
     * @author 황준서(37기) hzser123@gmail.com
     */
    @Override
    public JwtTokenInfo decodeToken(String token) {
        var now = new Date();
        DecodedJWT decodedJWT;
        JwtTokenInfo tokenInfo;
        try {
            decodedJWT = verifier.verify(token);

            //None 공격 방지를 위해 알고리즘을 반드시 검사해야한다.
            assert decodedJWT.getAlgorithm().equals("RS256");
            assert decodedJWT.getExpiresAt().after(now);
            assert decodedJWT.getSubject().equals("accessToken") || decodedJWT.getSubject().equals("refreshToken");

            tokenInfo = JwtTokenInfo.builder()
                    .uid(decodedJWT.getClaim("uid").asString())
                    .email(decodedJWT.getClaim("email").asString())
                    .role(decodedJWT.getClaim("role").asInt())
                    .isAccessToken(decodedJWT.getSubject().equals("accessToken"))
                    .expiredAt(decodedJWT.getExpiresAt())
                    .build();


        } catch (JWTVerificationException | AssertionError exception){
            throw new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"유효하지 않은 토큰입니다. 다시 로그인해주세요.");
        }
        return tokenInfo;
    }


    /**
     * 새로운 토큰을 발급한다.
     * @param jwtCreateTokenInfo 토큰을 발급할 유저의 정보
     * @param isAccessToken accessToken을 발급한건지 여부, 만약 false면 refresh token을 발급한다.
     * @return JwtCreateTokenResult 생성된 토큰과 유효시간을 반환한다.
     * @author 황준서(37기) hzser123@gmail.com
     */
    @Override
    public JwtCreateTokenResult createToken(JwtCreateTokenInfo jwtCreateTokenInfo,boolean isAccessToken) {

        var now = new Date();
        var tokenExpired = (isAccessToken) ? new Date(now.getTime() + Duration.ofSeconds(accessTokenExpiredSec).toMillis()) : new Date(now.getTime() + Duration.ofSeconds(refreshTokenExpiredSec).toMillis());

        String token = JWT.create()
                .withIssuer("jaramgroupware")
                .withIssuedAt(now)
                .withExpiresAt(tokenExpired)
                .withSubject((isAccessToken)? "accessToken" : "refreshToken")
                .withClaim("uid", jwtCreateTokenInfo.getUid())
                .withClaim("email", jwtCreateTokenInfo.getEmail())
                .withClaim("role",jwtCreateTokenInfo.getRole())
                .sign(algorithm);

        return JwtCreateTokenResult.builder()
                .token(token)
                .expiredAt(tokenExpired)
                .build();
    }


    /**
     * 주어진 토큰을 decode하고 해당 토큰을 검증한다.
     * @param token decode 및 검증할 토큰
     * @param jwtTokenVerifyInfo 검증할 정보
     * @throws JGWAuthException 해당 토큰이 유효하지 않다면 NOT_VALID_TOKEN 발생
     * @return JwtTokenInfo decode 된 토큰의 정보
     * @author 황준서(37기) hzser123@gmail.com
     */
    @Override
    public JwtTokenInfo verifyToken(String token, JwtTokenVerifyInfo jwtTokenVerifyInfo) {
        var tokenInfo =  decodeToken(token);
        try {

            assert tokenInfo.getRole().equals(jwtTokenVerifyInfo.getRole());
            assert tokenInfo.getEmail().equals(jwtTokenVerifyInfo.getEmail());
            assert tokenInfo.getUid().equals(jwtTokenVerifyInfo.getUid());
            assert tokenInfo.isAccessToken() == jwtTokenVerifyInfo.isAccessToken();

        } catch (AssertionError exception){
            throw new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"유효하지 않은 토큰입니다. 다시 로그인해주세요.");
        }
        return tokenInfo;
    }

    /**
     * 주어진 토큰을 decode하고 해당 토큰을 검증한다.
     * @param token decode 및 검증할 토큰
     * @param isAccessToken 해당 토큰이 accessToken인지 여부, false면 refresh token인지 검증한다.
     * @throws JGWAuthException 해당 토큰이 유효하지 않다면 NOT_VALID_TOKEN 발생
     * @return JwtTokenInfo decode 된 토큰의 정보
     * @author 황준서(37기) hzser123@gmail.com
     */
    @Override
    public JwtTokenInfo verifyToken(String token, boolean isAccessToken) {
        var tokenInfo =  decodeToken(token);
        try {
            assert tokenInfo.isAccessToken() == isAccessToken;
        } catch (AssertionError exception){
            throw new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"유효하지 않은 토큰입니다. 다시 로그인해주세요.");
        }
        return tokenInfo;
    }
}
