package com.sist.backend.exception;

/**
 * S3 파일 업로드 실패 예외
 */
public class S3UploadException extends S3Exception {
    
    public S3UploadException(String message) {
        super(message, "S3_UPLOAD_ERROR", "UPLOAD");
    }
    
    public S3UploadException(String message, Throwable cause) {
        super(message, "S3_UPLOAD_ERROR", "UPLOAD", cause);
    }
    
    public S3UploadException(String fileName, String message) {
        super(String.format("파일 업로드 실패 [%s]: %s", fileName, message), 
              "S3_UPLOAD_ERROR", "UPLOAD");
    }
    
    public S3UploadException(String fileName, String message, Throwable cause) {
        super(String.format("파일 업로드 실패 [%s]: %s", fileName, message), 
              "S3_UPLOAD_ERROR", "UPLOAD", cause);
    }
}

