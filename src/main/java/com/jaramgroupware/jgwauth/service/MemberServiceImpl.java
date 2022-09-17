package com.jaramgroupware.jgwauth.service;

import com.jaramgroupware.jgwauth.domain.jpa.member.Member;
import com.jaramgroupware.jgwauth.domain.jpa.member.MemberRepository;
import com.jaramgroupware.jgwauth.dto.member.serviceDto.MemberResponseServiceDto;
import com.jaramgroupware.jgwauth.utils.exception.CustomException;
import com.jaramgroupware.jgwauth.utils.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@RequiredArgsConstructor
@Service
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public Member findById(String id) {

        return memberRepository.findMemberById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
