package com.jaramgroupware.jgwauth.utils.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {


    MEMBER_NOT_FOUND(HttpStatus.FORBIDDEN,"MEMBER_NOT_FOUND","등록되지 않은 유저입니다."),
    NOT_VERIFIED_EMAIL(HttpStatus.FORBIDDEN,"MEMBER_NOT_FOUND","등록되지 않은 유저입니다.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String detail;

}
