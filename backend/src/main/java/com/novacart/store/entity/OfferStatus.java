package com.novacart.store.entity;

public enum OfferStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COUNTERED,
    WITHDRAWN,
    EXPIRED,
    /** An accepted offer that was used to create an order — cannot be reused. */
    CONSUMED
}
