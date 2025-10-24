package com.sist.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 파일 업로드 관련 예외를 처리하는 글로벌 예외 핸들러
 */
@RestControllerAdvice
public class FileUploadExceptionHandler {

    /**
     * 파일 업로드 크기 초과 예외 처리
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.put("error", "File Upload Size Exceeded");
        response.put("errorCode", "FILE_SIZE_EXCEEDED");
        response.put("message", "업로드할 파일의 크기가 너무 큽니다. 최대 10MB까지 업로드 가능합니다.");
        response.put("maxSize", "10MB");
        response.put("path", "/api/image/upload");
        
        System.err.println("❌ 파일 업로드 크기 초과: " + e.getMessage());
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }
}
