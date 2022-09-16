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

import java.util.concurrent.TimeUnit;

@ToString
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class MemberAuthAddRequestDto {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String token;
    private Boolean isValid;
    private Member member;
    private Long ttl;

    public MemberAuth toEntity() throws JsonProcessingException {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return MemberAuth.builder()
                .token(token)
                .member(objectMapper.writeValueAsString(member))
                .ttl(ttl)
                .isValid(isValid)
                .build();
    }

}
