package com.jaramgroupware.auth.dto.token.serviceDto;

import com.jaramgroupware.auth.dto.token.controllerDto.PublishAccessTokenResponseControllerDto;
import lombok.*;

import java.util.Date;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class PublishAccessTokenResponseServiceDto {
    private String accessToken;
    private Date accessTokenExpired;

    public PublishAccessTokenResponseControllerDto toControllerDto(){
        return PublishAccessTokenResponseControllerDto.builder()
                .accessToken(accessToken)
                .accessTokenExpired(accessTokenExpired)
                .build();
    }
}
