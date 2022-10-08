package com.jaramgroupware.jgwauth.web;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.firebase.auth.FirebaseAuthException;
import com.jaramgroupware.jgwauth.domain.jpa.member.Member;
import com.jaramgroupware.jgwauth.dto.auth.controllerDto.AuthFullResponseDto;
import com.jaramgroupware.jgwauth.dto.auth.controllerDto.AuthResponseDto;
import com.jaramgroupware.jgwauth.dto.auth.controllerDto.AuthTinyResponseDto;
import com.jaramgroupware.jgwauth.dto.general.controllerDto.MessageDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthAddRequestDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthResponseDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.TokenAuthAddRequestDto;
import com.jaramgroupware.jgwauth.service.MemberAuthServiceImpl;
import com.jaramgroupware.jgwauth.service.MemberServiceImpl;
import com.jaramgroupware.jgwauth.utils.firebase.FireBaseClientImpl;
import com.jaramgroupware.jgwauth.utils.firebase.FireBaseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.aspectj.bridge.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private final FireBaseClientImpl fireBaseClient;
    private final MemberAuthServiceImpl memberAuthService;
    private final MemberServiceImpl memberService;

    @GetMapping("")
    public ResponseEntity<AuthResponseDto> auth(
            @RequestParam(value = "onlyToken",defaultValue = "false") boolean isOnlyToken,
            @RequestHeader(value = "Token") String token
    ) throws FirebaseAuthException, JsonProcessingException {

        if(!isOnlyToken){
            return authFully(token);
        } else{
            return authTiny(token);
        }

    }

    public ResponseEntity<AuthResponseDto> authFully(String token) throws JsonProcessingException {
        MemberAuthResponseDto cacheResult = memberAuthService.find(token).orElse(null);
        //hit
        if(cacheResult != null){
            log.debug("Token hit. (Token={})",token);
            log.debug("cache info {}",cacheResult.toString());
            return ResponseEntity.ok(cacheResult.toAuthResponseDto());
        }

        //miss
        log.debug("Token miss. (Token={})",token);

        FireBaseResult fireBaseResult;
        //token check
        try{
            fireBaseResult = fireBaseClient.checkToken(token);
        } catch (FirebaseAuthException ex){
            log.info("Token Auth Fail. (Token={})",token);
            memberAuthService.add(MemberAuthAddRequestDto.builder()
                    .token(token)
                    .ttl(1L)
                    .member(null)
                    .isValid(false)
                    .build());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        //find user info
        Member member = memberService.findById(fireBaseResult.getUid());

        //add cache
        memberAuthService.add(MemberAuthAddRequestDto.builder()
                .token(token)
                .ttl(fireBaseResult.getTtl())
                .member(member)
                .isValid(true)
                .build());

        return ResponseEntity.ok(AuthFullResponseDto
                .builder()
                .roleID(member.getRole().getId())
                .valid(true)
                .uid(member.getId())
                .build());
    }

    public ResponseEntity<AuthResponseDto> authTiny(String token){
        String cacheResult = memberAuthService.findOnlyToken(token).orElse(null);
        //hit
        if(cacheResult != null){
            log.debug("Token hit. (Token={})",token);
            log.debug("cache info {}",cacheResult);
            return ResponseEntity.ok(AuthTinyResponseDto
                    .builder()
                    .valid(cacheResult.length() >= 1)
                    .uid(cacheResult)
                    .build());
        }

        //miss
        log.debug("Token miss. (Token={})",token);

        FireBaseResult fireBaseResult;
        //token check
        try{
            fireBaseResult = fireBaseClient.checkToken(token);
        } catch (FirebaseAuthException ex){
            log.info("Token Auth Fail. (Token={})",token);
            memberAuthService.add(TokenAuthAddRequestDto.builder()
                    .token(token)
                    .uid("")
                    .ttl(1L)
                    .build());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("firebase res (Token={})",fireBaseResult.toString());
        //add cache
        memberAuthService.add(TokenAuthAddRequestDto.builder()
                .token(token)
                .uid(fireBaseResult.getUid())
                .ttl(1L)
                .build());

        return ResponseEntity.ok(AuthTinyResponseDto
                .builder()
                .uid(fireBaseResult.getUid())
                .valid(true)
                .build());
    }

    @DeleteMapping("")
    public ResponseEntity<MessageDto> revoke(@RequestHeader(value = "Token") String token) throws FirebaseAuthException, JsonProcessingException {
        boolean res = memberAuthService.revoke(token);

        if(!res) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(new MessageDto("ok"));
    }
}

