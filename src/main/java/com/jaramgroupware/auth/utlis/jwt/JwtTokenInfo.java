package com.jaramgroupware.auth.utlis.jwt;

import lombok.*;

import java.util.Date;

@ToString
@EqualsAndHashCode
@Builder
@AllArgsConstructor
@Getter
@Setter
public class JwtTokenInfo {
    private String uid;
    private Integer role;
    private String email;
    private boolean isAccessToken;
    private Date expiredAt;
}
