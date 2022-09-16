package com.jaramgroupware.jgwauth.dto.memberCache.servcieDto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jaramgroupware.jgwauth.domain.jpa.member.Member;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuth;
import com.jaramgroupware.jgwauth.dto.auth.controllerDto.AuthResponseDto;
import lombok.*;

@ToString
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class MemberAuthResponseDto {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String token;
    private Boolean isValid;
    private String member;
    private Long ttl;

    public AuthResponseDto toAuthResponseDto() throws JsonProcessingException {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Member parseMember = objectMapper.readValue(member,Member.class);

        return AuthResponseDto.builder()
                .uid(parseMember.getId())
                .isValid(isValid)
                .roleID(parseMember.getRole().getId())
                .build();
    }

}
