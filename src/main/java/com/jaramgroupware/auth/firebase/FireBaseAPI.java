package com.jaramgroupware.auth.firebase;

import com.google.firebase.auth.FirebaseToken;

public interface FireBaseAPI {

    FireBaseTokenInfo checkToken(String token);
    Boolean revokeToken(String uid);
}
