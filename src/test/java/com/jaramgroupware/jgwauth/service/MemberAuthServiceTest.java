package com.jaramgroupware.jgwauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuth;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuthRepository;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthAddRequestDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthResponseDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.TokenAuthAddRequestDto;
import com.jaramgroupware.jgwauth.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ComponentScan
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MemberAuthServiceTest {

    @InjectMocks
    private MemberAuthServiceImpl memberAuthService;

    @Mock
    private MemberAuthRepository memberAuthRepository;

    @Mock
    private RedisTemplate<String,String> redisTemplate;

    private final TestUtils testUtils = new TestUtils();

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void add() throws JsonProcessingException {
        //given
        MemberAuthAddRequestDto memberCacheAddRequestDto = MemberAuthAddRequestDto.builder()
                .isValid(true)
                .member(testUtils.getTestMember())
                .ttl(10L)
                .token(testUtils.getTestToken())
                .build();

        //when
        Boolean result = memberAuthService.add(memberCacheAddRequestDto);

        //then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.toString(), Objects.requireNonNull(result).toString());
        verify(memberAuthRepository).save(memberCacheAddRequestDto.toEntity());
    }

    @Test
    void addToken(){
        //given
        TokenAuthAddRequestDto tokenAuthAddRequestDto = TokenAuthAddRequestDto.builder()
                .token(testUtils.getTestToken())
                .uid(testUtils.getTestUid())
                .ttl(10L)
                .build();
        ValueOperations valueOperations = Mockito.mock(ValueOperations.class);
        doReturn(valueOperations).when(redisTemplate).opsForValue();
        //when
        Boolean result = memberAuthService.add(tokenAuthAddRequestDto);

        //then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.toString(), Objects.requireNonNull(result).toString());
        verify(valueOperations).set("only_"+tokenAuthAddRequestDto.getToken(),tokenAuthAddRequestDto.getUid());
        verify(redisTemplate).expire("only_"+tokenAuthAddRequestDto.getToken(), Duration.ofMinutes(tokenAuthAddRequestDto.getTtl()));
    }

    @Test
    void find() throws JsonProcessingException {
        //given
        MemberAuthAddRequestDto memberCacheAddRequestDto = MemberAuthAddRequestDto.builder()
                .isValid(true)
                .member(testUtils.getTestMember())
                .ttl(10L)
                .token(testUtils.getTestToken())
                .build();

        MemberAuth target = memberCacheAddRequestDto.toEntity();

        doReturn(Optional.of(target)).when(memberAuthRepository).findById(target.getToken());
        //when
        MemberAuthResponseDto result = memberAuthService.find(testUtils.getTestToken()).orElseThrow();

        //then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.toString(), Objects.requireNonNull(result).toString());
        verify(memberAuthRepository).findById(target.getToken());
    }
    @Test
    void findById(){
        //given
        ValueOperations valueOperations = Mockito.mock(ValueOperations.class);
        doReturn(valueOperations).when(redisTemplate).opsForValue();
        doReturn(testUtils.getTestUid()).when(valueOperations).get("only_"+testUtils.getTestToken());

        //when
        String result = memberAuthService.findOnlyToken(testUtils.getTestToken()).orElseThrow();

        //then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result, testUtils.getTestUid());
        verify(valueOperations).get("only_"+testUtils.getTestToken());
    }
    @Test
    void revoke() throws JsonProcessingException {
        //given
        MemberAuthAddRequestDto memberCacheAddRequestDto = MemberAuthAddRequestDto.builder()
                .isValid(true)
                .member(testUtils.getTestMember())
                .ttl(10L)
                .token(testUtils.getTestToken())
                .build();

        MemberAuth target = memberCacheAddRequestDto.toEntity();
        Boolean targetSec = true;
        ValueOperations valueOperations = Mockito.mock(ValueOperations.class);
        doReturn(valueOperations).when(redisTemplate).opsForValue();
        doReturn(Optional.of(target)).when(memberAuthRepository).findById(target.getToken());
        doReturn("true").when(valueOperations).get("only_"+testUtils.getTestToken());
        doReturn(true).when(redisTemplate).delete("only_"+testUtils.getTestToken());

        //when
        memberAuthService.revoke(testUtils.getTestToken());

        //then
        verify(memberAuthRepository).findById(target.getToken());
        verify(valueOperations).get("only_"+testUtils.getTestToken());
        verify(memberAuthRepository).delete(target);
        verify(redisTemplate).delete("only_"+testUtils.getTestToken());
    }

}