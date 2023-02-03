package com.jaramgroupware.auth.dto.token.serviceDto;

import com.jaramgroupware.auth.dto.token.controllerDto.PublishTokenResponseControllerDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class PublishTokenResponseServiceDto {
    private String accessToken;
    private String refreshToken;
    private Date accessTokenExpired;
    private Date refreshTokenExpired;

    public PublishTokenResponseControllerDto toControllerDto(){
        return PublishTokenResponseControllerDto.builder()
                .accessToken(accessToken)
                .accessTokenExpired(accessTokenExpired)
                .refreshTokenExpired(refreshTokenExpired)
                .build();
    }

}
