package com.jaramgroupware.jgwauth.dto.member.serviceDto;

import com.jaramgroupware.jgwauth.domain.jpa.member.Member;
import lombok.*;

@ToString
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class MemberResponseServiceDto {
    private String id;
    private Integer roleID;

    public MemberResponseServiceDto(Member member){
        id = member.getId();
        roleID = member.getRole().getId();
    }
}
