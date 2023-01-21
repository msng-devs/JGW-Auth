package com.jaramgroupware.auth.firebase;

public interface FireBaseAPI {

    String checkTokenAndGetUid(String token);
    Boolean revokeToken(String uid);
}
