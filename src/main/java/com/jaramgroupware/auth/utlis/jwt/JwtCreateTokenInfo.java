package com.jaramgroupware.auth.utlis.jwt;

import lombok.*;

@ToString
@EqualsAndHashCode
@Builder
@AllArgsConstructor
@Getter
@Setter
public class JwtCreateTokenInfo {
    private String uid;
    private Integer role;
    private String email;
}
