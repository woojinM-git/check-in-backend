package com.sist.backend.exception;

/**
 * S3 파일 목록 조회 실패 예외
 */
public class S3ListException extends S3Exception {
    
    public S3ListException(String message) {
        super(message, "S3_LIST_ERROR", "LIST");
    }
    
    public S3ListException(String message, Throwable cause) {
        super(message, "S3_LIST_ERROR", "LIST", cause);
    }
    
    public S3ListException(String prefix, String message) {
        super(String.format("파일 목록 조회 실패 [prefix: %s]: %s", prefix, message), 
              "S3_LIST_ERROR", "LIST");
    }
    
    public S3ListException(String prefix, String message, Throwable cause) {
        super(String.format("파일 목록 조회 실패 [prefix: %s]: %s", prefix, message), 
              "S3_LIST_ERROR", "LIST", cause);
    }
}

