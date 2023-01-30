package com.jaramgroupware.auth.dto.token.serviceDto;

import lombok.*;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class PublishTokenRequestServiceDto {
    private String userUID;
    private Integer roleID;
    private String email;
}
