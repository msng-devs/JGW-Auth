package com.jaramgroupware.auth.web;

import com.jaramgroupware.auth.dto.auth.controllerDto.AuthResponseDto;
import com.jaramgroupware.auth.dto.general.controllerDto.MessageDto;
import com.jaramgroupware.auth.dto.member.serviceDto.MemberResponseServiceDto;
import com.jaramgroupware.auth.dto.token.controllerDto.AccessTokenResponseControllerDto;
import com.jaramgroupware.auth.dto.token.controllerDto.TokenResponseControllerDto;


import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenRequestServiceDto;
import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenResponseServiceDto;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthErrorCode;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthException;
import com.jaramgroupware.auth.firebase.FireBaseApiImpl;
import com.jaramgroupware.auth.firebase.FireBaseTokenInfo;
import com.jaramgroupware.auth.service.MemberServiceImpl;
import com.jaramgroupware.auth.service.TokenServiceImpl;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v2/auth")
@RestController
public class AuthApiController {

    private final TokenServiceImpl tokenService;
    private final MemberServiceImpl memberService;
    private final FireBaseApiImpl fireBaseApi;

    @Value("${jwt-key}")
    private String jwtSecret;

    @PostMapping("/authorization")
    public ResponseEntity<TokenResponseControllerDto> authorizationIdTokenAndPublishTokens(
            @RequestParam(value = "idToken",required = true) String idToken,
            HttpServletResponse response
    ){

        //firebase idToken 인증 후에 토큰을 모두 삭제시킴
        var fireBaseTokenInfo = fireBaseApi.checkToken(idToken);
        fireBaseApi.revokeToken(fireBaseTokenInfo.getUid());
        tokenService.blockFirebaseIdToken(fireBaseTokenInfo);

        var targetMember = memberService.findUserByUid(fireBaseTokenInfo.getUid());

        var publishTokenRequestServiceDto = PublishTokenRequestServiceDto.builder()
                .email(targetMember.getEmail())
                .roleID(targetMember.getRoleId())
                .userUID(targetMember.getUid())
                .build();

        var tokens = tokenService.publishToken(publishTokenRequestServiceDto);

        //xss 방어를 위해 access token만 response body로 전달하고, refresh token은 http only cookie에 저장함.
        var result = TokenResponseControllerDto.builder()
                .accessToken(tokens.getAccessToken())
                .accessTokenExpired(tokens.getAccessTokenExpired())
                .refreshTokenExpired(tokens.getRefreshTokenExpired())
                .build();

        var refreshCookie = createHttpOnlyCookie("jgw_refresh",tokens.getRefreshToken());
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(result);

    }

    @PostMapping("/accessToken")
    public ResponseEntity<AccessTokenResponseControllerDto> publishAccessToken(
            @CookieValue("jgw_refresh") String refreshToken,
            @RequestHeader(value = "user_pk") String userUid
    ){

        String uid = tokenService.checkRefreshToken(refreshToken);
        if(!isTokenValid(refreshToken) && !uid.equals(userUid)) throw new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"refresh 토큰의 유효시간이 만료되었습니다. 다시 로그인하세요");

        //TODO 토큰발행 및 검증을 종합적으로 관리하는 클래스로 분리하기
        var accessTokenInfo = tokenService.publishAccessToken(
                PublishTokenRequestServiceDto.builder()
                        .userUID(Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(refreshToken).getBody().get("uid",String.class))
                        .roleID(Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(refreshToken).getBody().get("role",int.class))
                        .email(Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(refreshToken).getBody().get("email",String.class))
                        .build());

        return ResponseEntity.ok(AccessTokenResponseControllerDto.builder()
                        .accessToken(accessTokenInfo.getAccessToken())
                        .accessTokenExpired(accessTokenInfo.getAccessTokenExpired())
                        .build());
    }

    @DeleteMapping("/revoke")
    public ResponseEntity<MessageDto> publishAccessToken(
            @CookieValue("jgw_refresh") String refreshToken,
            @RequestParam(value = "accessToken") String accessToken,
            @RequestHeader(value = "user_pk") String userUid
    ){

        boolean isRefreshTokenRevoke = false;
        boolean isAccessTokenRevoke = false;

        String refreshTokenUID = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(refreshToken).getBody().get("uid",String.class);
        String accessTokenUID = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(accessToken).getBody().get("uid",String.class);

        if(isTokenValid(refreshToken) && userUid.equals(refreshTokenUID)) isRefreshTokenRevoke = tokenService.revokeRefreshToken(refreshToken);

        if(isTokenValid(accessTokenUID) && userUid.equals(accessToken)){
            Date expireTime = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(accessToken).getBody().getExpiration();
            isAccessTokenRevoke = tokenService.revokeAccessToken(accessToken,accessTokenUID,expireTime);
        }

        if(isAccessTokenRevoke&&isRefreshTokenRevoke) return ResponseEntity.ok(new MessageDto("성공적으로 모든 토큰이 취소되었습니다."));
        if(isAccessTokenRevoke||isRefreshTokenRevoke) return ResponseEntity.status(HttpStatusCode.valueOf(203)).body(new MessageDto("일부 유효하지 않은 토큰을 제외하고 처리를 완료했습니다."));

        return ResponseEntity.badRequest().body(new MessageDto("입력받은 토큰들이 유효하지 않습니다."));

    }

    @GetMapping("/checkAccessToken")
    public ResponseEntity<AuthResponseDto> checkToken(
            @RequestParam(value = "accessToken") String accessToken){

        boolean isNotBlocked = tokenService.checkAccessToken(accessToken);
        
        if(!isNotBlocked) 
            return ResponseEntity.ok(AuthResponseDto.builder()
                .uid(null)
                .valid(false)
                .roleID(null)
                .build());

        //TODO 토큰발행 및 검증을 종합적으로 관리하는 클래스로 분리하기
        return ResponseEntity.ok(
                AuthResponseDto.builder()
                        .uid(Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(accessToken).getBody().get("uid",String.class))
                        .roleID(Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(accessToken).getBody().get("role",Integer.class))
                        .valid(true)
                        .build());
    }

    private boolean isTokenValid(String token){

        var now = new Date();
        //TODO 토큰발행 및 검증을 종합적으로 관리하는 클래스로 분리하기
        Date expireTime = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getExpiration();
        return now.after(expireTime);

    }

    private Cookie createHttpOnlyCookie(String key,String value){
        var cookie = new Cookie(key,value);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");

        return cookie;
    }
}
