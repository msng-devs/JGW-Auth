package com.jaramgroupware.jgwauth.utils.firebase;

import lombok.*;

@ToString
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class FireBaseResult {
    private String uid;
    private Long ttl;
}
