package com.sist.backend.exception;

/**
 * S3 파일 다운로드 실패 예외
 */
public class S3DownloadException extends S3Exception {
    
    public S3DownloadException(String message) {
        super(message, "S3_DOWNLOAD_ERROR", "DOWNLOAD");
    }
    
    public S3DownloadException(String message, Throwable cause) {
        super(message, "S3_DOWNLOAD_ERROR", "DOWNLOAD", cause);
    }
    
    public S3DownloadException(String fileKey, String message) {
        super(String.format("파일 다운로드 실패 [%s]: %s", fileKey, message), 
              "S3_DOWNLOAD_ERROR", "DOWNLOAD");
    }
    
    public S3DownloadException(String fileKey, String message, Throwable cause) {
        super(String.format("파일 다운로드 실패 [%s]: %s", fileKey, message), 
              "S3_DOWNLOAD_ERROR", "DOWNLOAD", cause);
    }
}

