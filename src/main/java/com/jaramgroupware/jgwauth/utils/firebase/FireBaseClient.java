package com.jaramgroupware.jgwauth.utils.firebase;

import com.google.firebase.auth.FirebaseAuthException;

public interface FireBaseClient {
    FireBaseResult checkToken(String token) throws FirebaseAuthException;
    String registerUser(String email,String password) throws FirebaseAuthException;
}
