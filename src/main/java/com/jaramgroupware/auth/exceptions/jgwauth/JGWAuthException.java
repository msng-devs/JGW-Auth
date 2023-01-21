package com.jaramgroupware.auth.exceptions.jgwauth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class JGWAuthException extends RuntimeException{
    private final JGWAuthErrorCode jgwAuthErrorCode;
    private final String detail;
}
