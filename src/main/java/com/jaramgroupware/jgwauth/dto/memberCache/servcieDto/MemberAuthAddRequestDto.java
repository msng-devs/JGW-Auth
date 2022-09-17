package com.jaramgroupware.jgwauth.dto.memberCache.servcieDto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jaramgroupware.jgwauth.domain.jpa.member.Member;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuth;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.TimeToLive;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@ToString
@Getter
@AllArgsConstructor
@Builder
public class MemberAuthAddRequestDto {

    private String token;
    private Boolean isValid;
    private Member member;
    private Long ttl;

    public MemberAuth toEntity() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return MemberAuth.builder()
                .token(token)
                .member(objectMapper.writeValueAsString(member))
                .ttl(ttl)
                .isValid(isValid)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        MemberAuthAddRequestDto target = (MemberAuthAddRequestDto) o;
        return token.equals(target.getToken()) &&
                member.toString().equals(target.getMember().toString()) &&
                isValid == target.getIsValid();
    }
}
