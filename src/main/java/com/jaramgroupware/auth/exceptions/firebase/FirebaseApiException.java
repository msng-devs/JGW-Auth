package com.jaramgroupware.auth.exceptions.firebase;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class FirebaseApiException extends RuntimeException {

    private final FireBaseErrorCode fireBaseErrorCode;

}
