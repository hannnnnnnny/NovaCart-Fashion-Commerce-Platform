package com.novacart.store.exception;

/**
 * The caller is authenticated but not allowed to act on this resource
 * (ownership / participation failure). Maps to HTTP 403 — distinct from a
 * 400 state-rule violation, so authz probing isn't logged as validation noise.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
