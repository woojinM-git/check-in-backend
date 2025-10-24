package com.sist.backend.exception;

/**
 * S3 파일 삭제 실패 예외
 */
public class S3DeleteException extends S3Exception {
    
    public S3DeleteException(String message) {
        super(message, "S3_DELETE_ERROR", "DELETE");
    }
    
    public S3DeleteException(String message, Throwable cause) {
        super(message, "S3_DELETE_ERROR", "DELETE", cause);
    }
    
    public S3DeleteException(String fileKey, String message) {
        super(String.format("파일 삭제 실패 [%s]: %s", fileKey, message), 
              "S3_DELETE_ERROR", "DELETE");
    }
    
    public S3DeleteException(String fileKey, String message, Throwable cause) {
        super(String.format("파일 삭제 실패 [%s]: %s", fileKey, message), 
              "S3_DELETE_ERROR", "DELETE", cause);
    }
}

