package com.csis231.api.common;

import lombok.Getter;

/**
 * Base runtime exception for domain-level errors.
 */
@Getter
public class ApplicationException extends RuntimeException {
    private final String code;

    public ApplicationException(String message) {
        super(message);
        this.code = null;
    }

    public ApplicationException(String message, String code) {
        super(message);
        this.code = code;
    }
}
