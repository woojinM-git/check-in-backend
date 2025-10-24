package com.sist.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.sist.backend.util.S3ImageService;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
@Tag(name = "이미지", description = "이미지 관련 API")
public class ImageController {
  private final S3ImageService imageService;

  @PostMapping("/s3/upload")
  public ResponseEntity<?> uploadImage(@RequestPart(value = "image", required = false) MultipartFile image){
    String imageUrl = imageService.upload(image);
    return ResponseEntity.ok(imageUrl);
  }
}