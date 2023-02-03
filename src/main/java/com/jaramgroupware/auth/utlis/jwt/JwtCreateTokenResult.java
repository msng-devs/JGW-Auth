package com.jaramgroupware.auth.utlis.jwt;

import lombok.*;

import java.util.Date;

@ToString
@EqualsAndHashCode
@Builder
@AllArgsConstructor
@Getter
@Setter
public class JwtCreateTokenResult {
    private String token;
    private Date expiredAt;
}
