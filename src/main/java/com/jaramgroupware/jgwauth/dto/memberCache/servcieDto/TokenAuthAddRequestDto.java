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
public class TokenAuthAddRequestDto {

    private String token;
    private String uid;
    private Long ttl;

    @Override
    public boolean equals(Object o) {
        TokenAuthAddRequestDto target = (TokenAuthAddRequestDto) o;
        return token.equals(target.getToken()) &&
                uid.equals(target.getUid());
    }
}
