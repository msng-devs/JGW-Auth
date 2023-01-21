package com.jaramgroupware.auth.exceptions.firebase;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FireBaseErrorCode {


    NOT_VERIFIED_EMAIL(HttpStatus.FORBIDDEN,"NOT_VERIFIED_EMAIL","이메일이 인증되지 않는 유저입니다."),
    NOT_VALID_TOKEN(HttpStatus.FORBIDDEN,"NOT_VALID_TOKEN","토큰 인증에 실패했습니다. 해당 토큰의 형식이 잘못되었거나, 사용가능한 토큰이 아닙니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;

}
