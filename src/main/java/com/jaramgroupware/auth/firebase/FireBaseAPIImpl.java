package com.jaramgroupware.auth.firebase;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.jaramgroupware.auth.exceptions.firebase.FireBaseErrorCode;
import com.jaramgroupware.auth.exceptions.firebase.FirebaseApiException;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthErrorCode;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class FireBaseAPIImpl implements FireBaseAPI {

    private final FirebaseAuth firebaseAuth;

    @Override
    public String checkTokenAndGetUid(String token) {
        FirebaseToken result = null;

        try{
            result = firebaseAuth.verifyIdToken(token,true);
        } catch (FirebaseAuthException e){
            processingFireBaseAuthException(e.getAuthErrorCode());
        } catch (IllegalArgumentException e){
            throw new JGWAuthException(JGWAuthErrorCode.INTERNAL_SERVER_ERROR,"토큰을 인증하는 중, firebase 서버와 통신중 오류가 발생했습니다. 관리자에게 문의하세요");
        }

        if(!result.isEmailVerified()) throw new FirebaseApiException(FireBaseErrorCode.NOT_VERIFIED_EMAIL);

        return result.getUid();
    }

    @Override
    public Boolean revokeToken(String uid) {

        try{
            firebaseAuth.revokeRefreshTokens(uid);
        } catch (FirebaseAuthException e){
            processingFireBaseAuthException(e.getAuthErrorCode());
        } catch (IllegalArgumentException e){
            throw new JGWAuthException(JGWAuthErrorCode.INTERNAL_SERVER_ERROR,"토큰을 삭제하는 과정에서, firebase 서버와 통신중 오류가 발생했습니다. 관리자에게 문의하세요");
        }

        return true;
    }

    private void processingFireBaseAuthException(AuthErrorCode authErrorCode){
        switch (authErrorCode){

            case EXPIRED_ID_TOKEN,INVALID_ID_TOKEN,REVOKED_ID_TOKEN,TENANT_ID_MISMATCH,CERTIFICATE_FETCH_FAILED,TENANT_NOT_FOUND,USER_DISABLED,USER_NOT_FOUND -> throw new FirebaseApiException(FireBaseErrorCode.NOT_VALID_TOKEN);

            default -> throw new JGWAuthException(JGWAuthErrorCode.INTERNAL_SERVER_ERROR,"토큰을 인증하는 중, firebase 서버와 통신중 오류가 발생했습니다. 관리자에게 문의하세요");
        }
    }

}
