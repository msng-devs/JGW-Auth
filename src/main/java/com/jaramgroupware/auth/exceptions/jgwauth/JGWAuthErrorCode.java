package com.jaramgroupware.auth.exceptions.jgwauth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum JGWAuthErrorCode {


    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_SERVER_ERROR","인증 서버에 오류가 발생했습니다."),
    NOT_VALID_TOKEN(HttpStatus.FORBIDDEN,"NOT_VALID_TOKEN","토큰 인증에 실패했습니다. 해당 토큰의 형식이 잘못되었거나, 사용가능한 토큰이 아닙니다."),
    NOT_FOUND_USER(HttpStatus.FORBIDDEN,"NOT_FOUND_USER","해당 유저를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String message;

}
