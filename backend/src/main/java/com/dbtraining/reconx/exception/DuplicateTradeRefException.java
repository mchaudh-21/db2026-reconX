package com.dbtraining.reconx.exception;

/** TICKET-ADV025 — 409 Conflict: tradeRef already exists. */
public class DuplicateTradeRefException extends ReconException {

    /**
 * Creates an exception for a duplicate trade reference.
 *
 * @param tradeRef the duplicate trade reference
 */
    public DuplicateTradeRefException(String tradeRef) {
        super("Duplicate tradeRef: " + tradeRef);
    }

/**
 * Creates an exception for a duplicate trade reference with an underlying cause.
 *
 * @param tradeRef the duplicate trade reference
 * @param cause the underlying cause
 */
    public DuplicateTradeRefException(String tradeRef, Throwable cause) {
        super("Duplicate tradeRef: " + tradeRef, cause);
    }
}
