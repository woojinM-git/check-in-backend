package com.sist.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * S3 관련 예외를 처리하는 글로벌 예외 핸들러
 */
@RestControllerAdvice
public class S3ExceptionHandler {

    /**
     * S3 업로드 예외 처리
     */
    @ExceptionHandler(S3UploadException.class)
    public ResponseEntity<Map<String, Object>> handleS3UploadException(S3UploadException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "S3 Upload Error");
        response.put("errorCode", e.getErrorCode());
        response.put("operation", e.getOperation());
        response.put("message", e.getMessage());
        response.put("path", "/api/s3/upload");
        
        System.err.println("❌ S3 업로드 예외 발생: " + e.getMessage());
        if (e.getCause() != null) {
            System.err.println("원인: " + e.getCause().getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * S3 다운로드 예외 처리
     */
    @ExceptionHandler(S3DownloadException.class)
    public ResponseEntity<Map<String, Object>> handleS3DownloadException(S3DownloadException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "S3 Download Error");
        response.put("errorCode", e.getErrorCode());
        response.put("operation", e.getOperation());
        response.put("message", e.getMessage());
        response.put("path", "/api/s3/download");
        
        System.err.println("❌ S3 다운로드 예외 발생: " + e.getMessage());
        if (e.getCause() != null) {
            System.err.println("원인: " + e.getCause().getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * S3 삭제 예외 처리
     */
    @ExceptionHandler(S3DeleteException.class)
    public ResponseEntity<Map<String, Object>> handleS3DeleteException(S3DeleteException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "S3 Delete Error");
        response.put("errorCode", e.getErrorCode());
        response.put("operation", e.getOperation());
        response.put("message", e.getMessage());
        response.put("path", "/api/s3/delete");
        
        System.err.println("❌ S3 삭제 예외 발생: " + e.getMessage());
        if (e.getCause() != null) {
            System.err.println("원인: " + e.getCause().getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * S3 목록 조회 예외 처리
     */
    @ExceptionHandler(S3ListException.class)
    public ResponseEntity<Map<String, Object>> handleS3ListException(S3ListException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "S3 List Error");
        response.put("errorCode", e.getErrorCode());
        response.put("operation", e.getOperation());
        response.put("message", e.getMessage());
        response.put("path", "/api/s3/list");
        
        System.err.println("❌ S3 목록 조회 예외 발생: " + e.getMessage());
        if (e.getCause() != null) {
            System.err.println("원인: " + e.getCause().getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 일반 S3 예외 처리
     */
    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<Map<String, Object>> handleS3Exception(S3Exception e) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "S3 Error");
        response.put("errorCode", e.getErrorCode());
        response.put("operation", e.getOperation());
        response.put("message", e.getMessage());
        response.put("path", "/api/s3");
        
        System.err.println("❌ S3 일반 예외 발생: " + e.getMessage());
        if (e.getCause() != null) {
            System.err.println("원인: " + e.getCause().getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

