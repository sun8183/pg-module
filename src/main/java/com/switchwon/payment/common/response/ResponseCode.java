package com.switchwon.payment.common.response;

import org.springframework.http.HttpStatus;

public enum ResponseCode {

    OK(HttpStatus.OK, "정상적으로 처리되었습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 정보를 찾을 수 없습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY, "지갑 잔액이 부족합니다."),
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 시스템 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ResponseCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}