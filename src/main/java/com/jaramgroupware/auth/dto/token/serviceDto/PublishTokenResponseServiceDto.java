package com.jaramgroupware.auth.dto.token.serviceDto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PublishTokenResponseServiceDto {
    private String accessToken;
    private String refreshToken;
    private Date accessTokenExpired;
    private Date refreshTokenExpired;
}
