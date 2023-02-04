package com.jaramgroupware.auth.utlis.jwt;

public interface TokenManager {
    JwtTokenInfo decodeToken(String token);
    JwtCreateTokenResult createToken(JwtCreateTokenInfo jwtCreateTokenInfo,boolean isAccessToken);
    JwtTokenInfo verifyToken(String token,JwtTokenVerifyInfo jwtTokenVerifyInfo);
    JwtTokenInfo verifyToken(String token,boolean isAccessToken);
}
