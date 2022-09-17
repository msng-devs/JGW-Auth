package com.jaramgroupware.jgwauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuth;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuthRepository;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthAddRequestDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberAuthServiceImpl implements MemberAuthService {

    private final MemberAuthRepository memberAuthRepository;

    @Override
    @Transactional
    public boolean add(MemberAuthAddRequestDto memberCacheAddRequestDto) throws JsonProcessingException {

        memberAuthRepository.save(memberCacheAddRequestDto.toEntity());

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberAuthResponseDto> find(String token) {
        MemberAuth memberAuth = memberAuthRepository
                .findById(token)
                .orElse(null);

        if(memberAuth == null) return Optional.empty();

        return Optional.of(MemberAuthResponseDto.builder()
                .member(memberAuth.getMember())
                .isValid(memberAuth.getIsValid())
                .build());
    }

    @Override
    @Transactional
    public boolean revoke(String token) {
        MemberAuth memberAuth = memberAuthRepository.findById(token)
                .orElse(null);

        if(memberAuth == null) return false;

        memberAuthRepository.delete(memberAuth);
        return true;
    }
}
