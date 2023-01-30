package com.jaramgroupware.auth.dto.token.controllerDto;


import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.Date;

@ToString
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TokenResponseControllerDto {
    private String accessToken;
    private Date accessTokenExpired;
    private Date refreshTokenExpired;
}
