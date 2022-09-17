package com.jaramgroupware.jgwauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuth;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuthRepository;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthAddRequestDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthResponseDto;
import com.jaramgroupware.jgwauth.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

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
    void revoke() throws JsonProcessingException {
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
        Boolean result = memberAuthService.revoke(testUtils.getTestToken());

        //then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.toString(), Objects.requireNonNull(result).toString());
        verify(memberAuthRepository).findById(target.getToken());
        verify(memberAuthRepository).delete(target);
    }

}