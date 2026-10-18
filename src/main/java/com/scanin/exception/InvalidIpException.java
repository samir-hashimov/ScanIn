package com.scanin.exception;

public class InvalidIpException extends RuntimeException {
    public InvalidIpException(String message) {
        super(message);
    }
}