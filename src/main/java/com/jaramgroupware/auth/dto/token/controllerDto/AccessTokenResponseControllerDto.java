package com.jaramgroupware.auth.dto.token.controllerDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.Date;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AccessTokenResponseControllerDto {
    private String accessToken;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date accessTokenExpired;
}
