package com.jaramgroupware.auth.dto.token.serviceDto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PublishAccessTokenResponseServiceDto {
    private String accessToken;
    private Date accessTokenExpired;
}
