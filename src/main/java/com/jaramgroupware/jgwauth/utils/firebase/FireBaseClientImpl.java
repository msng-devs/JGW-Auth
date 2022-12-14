package com.jaramgroupware.jgwauth.utils.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.jaramgroupware.jgwauth.utils.exception.CustomException;
import com.jaramgroupware.jgwauth.utils.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

@Slf4j
@RequiredArgsConstructor
@Component
public class FireBaseClientImpl implements FireBaseClient {

    private final FirebaseAuth firebaseAuth;

    @Override
    public FireBaseResult checkToken(String token) throws FirebaseAuthException {

        FirebaseToken result = firebaseAuth.verifyIdToken(token);
        if(result.isEmailVerified()) throw new CustomException(ErrorCode.NOT_VERIFIED_EMAIL);
        LocalDateTime exp = Instant.ofEpochSecond((Long) result.getClaims().get("exp"))
                .atZone(ZoneId.systemDefault()).toLocalDateTime();

        log.info("Token verify Success Token = {}",result.getUid());
        return FireBaseResult.builder()
                .uid(result.getUid())
                .ttl(Duration.between(exp,LocalDateTime.now()).toMinutes())
                .build();
    }

    public String registerUser(String email,String password) throws FirebaseAuthException {
        UserRecord.CreateRequest newUser = new UserRecord.CreateRequest();
        newUser.setEmail(email);
        newUser.setEmailVerified(false);
        newUser.setPassword(password);

        UserRecord newUserRecord = firebaseAuth.createUser(newUser);

        return newUserRecord.getUid();
    }
}
