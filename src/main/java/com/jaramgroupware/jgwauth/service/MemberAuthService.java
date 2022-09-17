package com.jaramgroupware.jgwauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuth;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthAddRequestDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthResponseDto;

import java.util.Optional;

public interface MemberAuthService {

    boolean add(MemberAuthAddRequestDto memberCacheAddRequestDto) throws JsonProcessingException;
    Optional<MemberAuthResponseDto> find(String token);
    boolean revoke(String token);

}
