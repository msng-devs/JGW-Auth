package com.jaramgroupware.jgwauth.dto.memberCache.servcieDto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jaramgroupware.jgwauth.domain.jpa.member.Member;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuth;
import com.jaramgroupware.jgwauth.dto.auth.controllerDto.AuthFullResponseDto;
import com.jaramgroupware.jgwauth.dto.auth.controllerDto.AuthResponseDto;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
@ToString
@Getter
@AllArgsConstructor
@Builder
public class MemberAuthResponseDto {

    private String token;
    private Boolean isValid;
    private String member;
    private Long ttl;

    public AuthResponseDto toAuthResponseDto() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Member parseMember = objectMapper.readValue(member,Member.class);
        return AuthFullResponseDto.builder()
                .uid(parseMember.getId())
                .isValid(isValid)
                .roleID(parseMember.getRole().getId())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        MemberAuthResponseDto target = (MemberAuthResponseDto) o;
        return token.equals(target.getToken()) &&
                member.equals(target.getMember()) &&
                isValid == target.getIsValid();
    }
}
