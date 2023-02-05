package com.jaramgroupware.auth.firebase;

import com.google.firebase.auth.*;
import com.jaramgroupware.auth.exceptions.firebase.FireBaseErrorCode;
import com.jaramgroupware.auth.exceptions.firebase.FirebaseApiException;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthErrorCode;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Firebase Admin SDK를 사용하여 토큰을 검증 및 블락 하는 클래스
 * @since 2023-01-24
 * @author 황준서(37기) hzser123@gmail.com
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class FireBaseApiImpl implements FireBaseApi {

    private final FirebaseAuth firebaseAuth;

    /**
     * 입력 받은 FireBase ID Token을 검증하고, 해당 결과를 리턴한다.
     *
     * @param token Firebase ID Token(Access Token)
     * @return FireBaseTokenInfo(토큰,해당 토큰 유저의 UID,토큰 만료시간)
     * @throws FirebaseApiException 해당 토큰이 valid 하지 않거나, 이메일 인증을 받지 않은 유저면 발생
     * @throws JGWAuthException Firebase Admin SDK 사용중 오류가 발생하면 발생
     */
    @Override
    public FireBaseTokenInfo checkToken(String token) {
        FirebaseToken result = null;

        try{
            result = firebaseAuth.verifyIdToken(token,true);
        } catch (FirebaseAuthException e){
            processingFireBaseAuthException(e.getAuthErrorCode());
        } catch (IllegalArgumentException e){
            throw new JGWAuthException(JGWAuthErrorCode.INTERNAL_SERVER_ERROR,"토큰을 인증하는 중, firebase 서버와 통신중 오류가 발생했습니다. 관리자에게 문의하세요");
        }

        if(!result.isEmailVerified()) throw new FirebaseApiException(FireBaseErrorCode.NOT_VERIFIED_EMAIL);

        return FireBaseTokenInfo.builder()
                .idToken(token)
                .uid(result.getUid())
                .expireDateTime(Instant.ofEpochSecond((Long) result.getClaims().get("exp")).atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }

    /**
     * 입력 받은 Uid에 해당하는 유저의 refresh token을 취소시킴
     *
     * !access token은 취소가 불가능하니 참고할 것.
     *
     * @param uid 대상 유저의 UID
     * @throws FirebaseApiException 해당 유저 정보가 valid 하지 않거나, 이메일 인증을 받지 않은 유저면 발생
     * @throws JGWAuthException Firebase Admin SDK 사용중 오류가 발생하면 발생
     */
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

    /**
     * 입력 받은 Firebase의 오류 코드에 따라 에러처리
     *
     * @param authErrorCode Firebase Admin SDK의 에러 코드
     * @throws FirebaseApiException Firebase Admin SDK 쪽 문제일 경우 발생
     * @throws JGWAuthException 서버 내부 오류일경우 발생
     */
    private void processingFireBaseAuthException(AuthErrorCode authErrorCode){
        //상세한 에러 코드는 다음 링크 참고, ref  : https://firebase.google.com/docs/reference/admin/java/reference/com/google/firebase/auth/AuthErrorCode
        switch (authErrorCode){

            case EXPIRED_ID_TOKEN,INVALID_ID_TOKEN,REVOKED_ID_TOKEN,TENANT_ID_MISMATCH,CERTIFICATE_FETCH_FAILED,TENANT_NOT_FOUND,USER_DISABLED,USER_NOT_FOUND -> throw new FirebaseApiException(FireBaseErrorCode.NOT_VALID_TOKEN);

            default -> throw new JGWAuthException(JGWAuthErrorCode.INTERNAL_SERVER_ERROR,"토큰을 인증하는 중, firebase 서버와 통신중 오류가 발생했습니다. 관리자에게 문의하세요");
        }
    }

    public void indexUserMakeEmail(String idToken) throws FirebaseAuthException {
        FirebaseToken result = firebaseAuth.verifyIdToken(idToken,true);
        UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(result.getUid())
                .setEmailVerified(true);

        firebaseAuth.updateUser(request);
    }
}
