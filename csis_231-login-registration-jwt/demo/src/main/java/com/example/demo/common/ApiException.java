package com.example.demo.common;

/**
 * Client-side exception representing an HTTP error response.
 */
public class ApiException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;

    public ApiException(int statusCode, String message, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public ApiException(int statusCode, String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
