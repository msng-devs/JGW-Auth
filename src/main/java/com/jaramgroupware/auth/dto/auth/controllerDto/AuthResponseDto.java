package com.jaramgroupware.auth.dto.auth.controllerDto;


import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AuthResponseDto {
    private Boolean valid;
    private String uid;
    private Integer roleID;
}
