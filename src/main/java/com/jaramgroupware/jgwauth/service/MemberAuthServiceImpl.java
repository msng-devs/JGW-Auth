package com.jaramgroupware.jgwauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuth;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuthRepository;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthAddRequestDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthResponseDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.TokenAuthAddRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberAuthServiceImpl implements MemberAuthService {

    private final MemberAuthRepository memberAuthRepository;
    private final RedisTemplate<String,String> redisTemplate;

    @Override
    @Transactional
    public boolean add(MemberAuthAddRequestDto memberCacheAddRequestDto) throws JsonProcessingException {

        memberAuthRepository.save(memberCacheAddRequestDto.toEntity());

        return true;
    }

    @Override
    @Transactional
    public Boolean add(TokenAuthAddRequestDto tokenAuthAddRequestDto){
        ValueOperations<String,String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set("only_"+tokenAuthAddRequestDto.getToken(),tokenAuthAddRequestDto.getUid());
        redisTemplate.expire("only_"+tokenAuthAddRequestDto.getToken(), Duration.ofMinutes(tokenAuthAddRequestDto.getTtl()));

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
    @Transactional(readOnly = true)
    public Optional<String> findOnlyToken(String token) {
        ValueOperations<String,String> valueOperations = redisTemplate.opsForValue();
        String uid = valueOperations.get("only_"+token);
        if(uid == null) return Optional.empty();

        return Optional.of(uid);
    }

    @Override
    @Transactional
    public boolean revoke(String token) {
        MemberAuth memberAuth = memberAuthRepository.findById(token)
                .orElse(null);
        ValueOperations<String,String> valueOperations = redisTemplate.opsForValue();
        String onlyToken = valueOperations.get("only_"+token);

        if(memberAuth != null) memberAuthRepository.delete(memberAuth);
        if(onlyToken != null) redisTemplate.delete("only_"+token);

        return true;
    }
}
