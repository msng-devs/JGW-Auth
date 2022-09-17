package com.jaramgroupware.jgwauth.service;

import com.jaramgroupware.jgwauth.utils.TestUtils;
import com.jaramgroupware.jgwauth.domain.jpa.member.Member;
import com.jaramgroupware.jgwauth.domain.jpa.member.MemberRepository;
import com.jaramgroupware.jgwauth.dto.member.serviceDto.MemberResponseServiceDto;
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

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ComponentScan
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberServiceImpl memberService;

    @Mock
    private MemberRepository memberRepository;

    private final TestUtils testUtils = new TestUtils();

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }
    @Test
    void findById() {

        //given
        String testID = testUtils.getTestMember().getId();
        Member testEntity = testUtils.getTestMember();

        doReturn(Optional.of(testEntity)).when(memberRepository).findMemberById(testID);

        //when
        Member result = memberService.findById(testID);

        //then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(testEntity.toString(), Objects.requireNonNull(result).toString());
        verify(memberRepository).findMemberById(testID);
    }
}