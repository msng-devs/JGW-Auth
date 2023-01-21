package com.jaramgroupware.auth.service;

import com.jaramgroupware.auth.domain.Member;
import com.jaramgroupware.auth.domain.MemberRepository;
import com.jaramgroupware.auth.dto.member.serviceDto.MemberResponseServiceDto;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthErrorCode;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthException;
import com.jaramgroupware.auth.testUtils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Description;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ComponentScan
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberServiceImpl memberService;

    @Mock
    private MemberRepository memberRepository;

    private final TestUtils testUtils = new TestUtils();
    @Test
    @Description("올바른 유저의 id가 주어졌을 때, 해당 유저를 반환한다.")
    void findUserByUid() {
        //given

        Member targetMember = testUtils.getTestMember();

        MemberResponseServiceDto exceptResult = MemberResponseServiceDto.builder()
                .uid(targetMember.getId())
                .email(targetMember.getEmail())
                .roleId(targetMember.getRole())
                .build();

        doReturn(Optional.of(targetMember)).when(memberRepository).findById(targetMember.getId());

        //when

        MemberResponseServiceDto result = memberService.findUserByUid(targetMember.getId());

        //then
        assertDoesNotThrow(() -> new JGWAuthException(JGWAuthErrorCode.NOT_FOUND_USER,"해당 유저 정보 조회에 실패했습니다. 회원가입 절차를 완료해주세요."));
        assertEquals(exceptResult.toString(),result.toString());
        verify(memberRepository).findById(targetMember.getId());
    }

    @Test
    @Description("존재하지 않는 유저의 id가 주어졌을 때, JGWAuthException 에러를 발생시킨다.")
    void findUserByUid2() {
        //given
        String testUID = "doyouknowherosofthestorm";
        doReturn(Optional.empty()).when(memberRepository).findById(testUID);

        //when
        assertThrows(JGWAuthException.class,() ->{
            memberService.findUserByUid(testUID);
        });

    }

}