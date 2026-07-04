package com.novacart.store.exception;

/**
 * The request conflicts with the current state of the resource — e.g. a
 * concurrent buy-now / offer-accept lost an optimistic-lock race, or a
 * unique-constraint collision. Maps to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
