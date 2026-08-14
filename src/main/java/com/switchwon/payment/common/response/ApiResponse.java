package com.switchwon.payment.common.response;

public record ApiResponse<T>(String code, String message, T returnObject) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCode.OK.name(), ResponseCode.OK.getDefaultMessage(), data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(ResponseCode.OK.name(), ResponseCode.OK.getDefaultMessage(), null);
    }

    public static ApiResponse<Void> error(ResponseCode code) {
        return new ApiResponse<>(code.name(), code.getDefaultMessage(), null);
    }

    public static ApiResponse<Void> error(ResponseCode code, String message) {
        return new ApiResponse<>(code.name(), message, null);
    }
}