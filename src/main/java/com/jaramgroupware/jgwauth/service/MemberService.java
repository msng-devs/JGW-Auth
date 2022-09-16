package com.jaramgroupware.jgwauth.service;

import com.jaramgroupware.jgwauth.dto.member.serviceDto.MemberResponseServiceDto;
import org.springframework.stereotype.Service;

@Service
public interface MemberService {
    MemberResponseServiceDto findById(String id);
}
