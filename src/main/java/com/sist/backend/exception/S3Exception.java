package com.sist.backend.exception;

/**
 * S3 관련 예외를 처리하는 커스텀 예외 클래스
 */
public class S3Exception extends RuntimeException {
    
    private final String errorCode;
    private final String operation;
    
    public S3Exception(String message) {
        super(message);
        this.errorCode = "S3_ERROR";
        this.operation = "UNKNOWN";
    }
    
    public S3Exception(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "S3_ERROR";
        this.operation = "UNKNOWN";
    }
    
    public S3Exception(String message, String errorCode, String operation) {
        super(message);
        this.errorCode = errorCode;
        this.operation = operation;
    }
    
    public S3Exception(String message, String errorCode, String operation, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.operation = operation;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getOperation() {
        return operation;
    }
    
    @Override
    public String toString() {
        return String.format("S3Exception{errorCode='%s', operation='%s', message='%s'}", 
                           errorCode, operation, getMessage());
    }
}

