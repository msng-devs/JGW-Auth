package com.jaramgroupware.auth.service;

import com.jaramgroupware.auth.dto.member.serviceDto.MemberResponseServiceDto;

public interface MemberService {
    MemberResponseServiceDto findUserByUid(String uid);
}
