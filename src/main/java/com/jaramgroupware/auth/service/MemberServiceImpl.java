package com.jaramgroupware.auth.service;

import com.jaramgroupware.auth.domain.Member;
import com.jaramgroupware.auth.domain.MemberRepository;
import com.jaramgroupware.auth.dto.member.serviceDto.MemberResponseServiceDto;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthErrorCode;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    public MemberResponseServiceDto findUserByUid(String uid) {
        Member targetMember = memberRepository.findById(uid).orElseThrow(() -> new JGWAuthException(JGWAuthErrorCode.NOT_FOUND_USER,"해당 유저 정보 조회에 실패했습니다. 회원가입 절차를 완료해주세요."));

        return MemberResponseServiceDto
                .builder()
                .uid(targetMember.getId())
                .email(targetMember.getEmail())
                .roleId(targetMember.getRole())
                .build();
    }
}
