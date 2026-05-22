package com.codesync.payment.exception;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String message) { super(message); }
}
