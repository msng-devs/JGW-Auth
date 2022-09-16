package com.jaramgroupware.jgwauth.service;

import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthAddRequestDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthResponseDto;

import java.util.Optional;

public interface MemberAuthService {

    boolean add(MemberAuthAddRequestDto memberCacheAddRequestDto);
    Optional<MemberAuthResponseDto> find(String uid);
    boolean revoke(String uid);

}
