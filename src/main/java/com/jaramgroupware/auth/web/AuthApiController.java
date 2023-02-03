package com.jaramgroupware.auth.web;

import com.auth0.jwt.exceptions.JWTCreationException;
import com.jaramgroupware.auth.dto.auth.controllerDto.AuthResponseDto;
import com.jaramgroupware.auth.dto.general.controllerDto.MessageDto;
import com.jaramgroupware.auth.dto.token.controllerDto.PublishAccessTokenResponseControllerDto;
import com.jaramgroupware.auth.dto.token.controllerDto.PublishTokenResponseControllerDto;


import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenRequestServiceDto;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthException;
import com.jaramgroupware.auth.firebase.FireBaseApiImpl;
import com.jaramgroupware.auth.service.MemberServiceImpl;
import com.jaramgroupware.auth.service.TokenServiceImpl;
import com.jaramgroupware.auth.utlis.jwt.JwtTokenInfo;
import com.jaramgroupware.auth.utlis.jwt.JwtTokenVerifyInfo;
import com.jaramgroupware.auth.utlis.jwt.TokenManagementImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v2/auth")
@RestController
public class AuthApiController {

    private final TokenServiceImpl tokenService;
    private final MemberServiceImpl memberService;
    private final FireBaseApiImpl fireBaseApi;
    private final TokenManagementImpl tokenManagement;

    @PostMapping("/authorization")
    public ResponseEntity<PublishTokenResponseControllerDto> authorizationIdTokenAndPublishTokens(
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
        var result = tokens.toControllerDto();

        var refreshCookie = createHttpOnlyCookie("jgw_refresh",tokens.getRefreshToken());

        response.addCookie(refreshCookie);

        return ResponseEntity.ok(result);

    }

    @PostMapping("/accessToken")
    public ResponseEntity<PublishAccessTokenResponseControllerDto> publishAccessToken(
            @CookieValue("jgw_refresh") String refreshToken
    ){
        String uid = tokenService.checkRefreshToken(refreshToken);
        JwtTokenInfo jwtTokenInfo = tokenManagement.decodeToken(refreshToken);

        var accessTokenInfo = tokenService.publishAccessToken(
                PublishTokenRequestServiceDto.builder()
                        .userUID(jwtTokenInfo.getUid())
                        .roleID(jwtTokenInfo.getRole())
                        .email(jwtTokenInfo.getEmail())
                        .build());

        return ResponseEntity.ok(accessTokenInfo.toControllerDto());
    }

    @DeleteMapping("/revoke")
    public ResponseEntity<MessageDto> revokeTokens(
            @CookieValue("jgw_refresh") String refreshToken,
            @RequestParam(value = "accessToken") String accessToken,
            HttpServletResponse response){

        JwtTokenInfo refreshTokenInfo;

        try {
            refreshTokenInfo = tokenManagement.verifyToken(refreshToken,false);
        } catch (JGWAuthException | JWTCreationException | AssertionError exception){
            return ResponseEntity.badRequest().body(new MessageDto("유효하지 않은 토큰입니다."));
        }

        //기존에 저장된 refresh 토큰은 제거한다.
        var cookie = createHttpOnlyCookie("jgw_refresh",null);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        //accessToken을 검증하고 해당 토큰을 black list에 추가한다.
        try {
            JwtTokenInfo accessTokenInfo = tokenManagement.verifyToken(accessToken,
                    JwtTokenVerifyInfo.builder()
                            .uid(refreshTokenInfo.getUid())
                            .email(refreshTokenInfo.getEmail())
                            .isAccessToken(true)
                            .build());

            tokenService.revokeAccessToken(accessToken,accessTokenInfo.getUid(),accessTokenInfo.getExpiredAt());

        } catch (JGWAuthException | JWTCreationException | AssertionError exception){

            //위 refresh token은 성공적으로 삭제한 상황이므로, 203을 리턴한다.
            return ResponseEntity.status(203).body(new MessageDto("일부 토큰을 성공적으로 취소 했습니다."));
        }


        return ResponseEntity.ok(new MessageDto("성공적으로 모든 토큰을 삭제했습니다."));

    }

    @GetMapping("/checkAccessToken")
    public ResponseEntity<AuthResponseDto> checkToken(
            @RequestParam(value = "accessToken") String accessToken){

        JwtTokenInfo jwtTokenInfo = tokenManagement.decodeToken(accessToken);
        boolean isNotBlocked = tokenService.checkAccessToken(accessToken);

        return ResponseEntity.ok(
                AuthResponseDto.builder()
                        .uid(jwtTokenInfo.getUid())
                        .roleID(jwtTokenInfo.getRole())
                        .valid(isNotBlocked)
                        .build());
    }

    private Cookie createHttpOnlyCookie(String key,String value){
        var cookie = new Cookie(key,value);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");

        return cookie;
    }
}
