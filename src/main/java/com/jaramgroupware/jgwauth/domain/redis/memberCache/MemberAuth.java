package com.jaramgroupware.jgwauth.domain.redis.memberCache;

import com.jaramgroupware.jgwauth.domain.jpa.member.Member;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@RedisHash("memberAuth")
public class MemberAuth {

    @Id
    private String token;

    private Boolean isValid;
    private String member;

    @TimeToLive(unit = TimeUnit.MINUTES)
    private Long ttl;

    public boolean equalsExcludeTTL(MemberAuth memberAuth){
        return memberAuth.getToken().equals(token) &&
                memberAuth.getMember().equals(member) &&
                memberAuth.getIsValid().equals(isValid);
    }
}
