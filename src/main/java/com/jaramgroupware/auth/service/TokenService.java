package com.jaramgroupware.auth.service;

import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenRequestServiceDto;
import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenResponseServiceDto;
import com.jaramgroupware.auth.firebase.FireBaseTokenInfo;

import java.util.Date;

public interface TokenService {

    boolean blockFirebaseIdToken(FireBaseTokenInfo fireBaseTokenInfo);
    boolean checkFirebaseIdToken(String fireBaseToken);
    PublishTokenResponseServiceDto publishToken(PublishTokenRequestServiceDto publishTokenRequestDto);
    Boolean revokeAccessToken(String accessToken,String uid,Date expiredDate);
    Boolean revokeRefreshToken(String refreshToken);
    Boolean revokeUserToken(String userUID);
    boolean checkAccessToken(String accessToken);
    String checkRefreshToken(String refreshToken);
    PublishTokenResponseServiceDto publishAccessToken(PublishTokenRequestServiceDto publishTokenRequestDto);
}
