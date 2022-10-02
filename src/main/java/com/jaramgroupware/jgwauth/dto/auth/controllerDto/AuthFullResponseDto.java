package com.jaramgroupware.jgwauth.dto.auth.controllerDto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AuthFullResponseDto extends AuthResponseDto{
    private boolean isValid;
    private String uid;
    private Integer roleID;
}
