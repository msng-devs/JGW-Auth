package com.jaramgroupware.auth.firebase;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Firebase API Class에서 토큰의 정보를 리턴하기 위해 사용하는 클래스
 * @since 2023-01-24
 * @author 황준서(37기) hzser123@gmail.com
 */
@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FireBaseTokenInfo {
    private String idToken;
    private String uid;
    private LocalDateTime expireDateTime;
}
