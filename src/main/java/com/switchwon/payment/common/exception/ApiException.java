package com.switchwon.payment.common.exception;

import com.switchwon.payment.common.response.ResponseCode;

public class ApiException extends RuntimeException {

    private final ResponseCode responseCode;

    public ApiException(ResponseCode responseCode) {
        super(responseCode.getDefaultMessage());
        this.responseCode = responseCode;
    }

    public ApiException(ResponseCode responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }
}