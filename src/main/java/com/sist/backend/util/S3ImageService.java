package com.sist.backend.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;

import com.sist.backend.exception.S3UploadException;
import com.sist.backend.exception.S3DeleteException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

// 이 어노테이션은 롬복에서 제공하는 @Slf4j 어노테이션으로, 클래스 내에서 간편하게 로그를 남길 수 있도록 log 필드를 자동으로 생성해줍니다. 
// 예: log.info("메시지");
@Slf4j
@RequiredArgsConstructor
@Component
public class S3ImageService {

  private final AmazonS3 amazonS3;

  @Value("${cloud.aws.s3.bucketName}")
  private String bucketName;

  public String upload(MultipartFile image) {
    if(image.isEmpty() || Objects.isNull(image.getOriginalFilename())){
      throw new S3UploadException("업로드할 파일이 비어있습니다.");
    }
    return this.uploadImage(image, null, false);
  }

  /**
   * 폴더 경로를 지정하여 이미지를 업로드합니다.
   * @param image 업로드할 이미지 파일
   * @param folderPath S3 폴더 경로 (예: "hotelmain/hotel", "hotelroom")
   * @param returnFileNameOnly true면 파일명만 반환, false면 전체 URL 반환
   * @return 업로드된 이미지의 S3 URL 또는 파일명 (returnFileNameOnly에 따라)
   */
  public String upload(MultipartFile image, String folderPath, boolean returnFileNameOnly) {
    if(image.isEmpty() || Objects.isNull(image.getOriginalFilename())){
      throw new S3UploadException("업로드할 파일이 비어있습니다.");
    }
    return this.uploadImage(image, folderPath, returnFileNameOnly);
  }

  /**
   * 폴더 경로를 지정하여 이미지를 업로드합니다. (기본: 전체 URL 반환)
   * @param image 업로드할 이미지 파일
   * @param folderPath S3 폴더 경로 (예: "hotelmain/hotel", "hotelroom")
   * @return 업로드된 이미지의 S3 URL
   */
  public String upload(MultipartFile image, String folderPath) {
    return upload(image, folderPath, false);
  }

  private String uploadImage(MultipartFile image, String folderPath, boolean returnFileNameOnly) {
    this.validateImageFileExtention(image.getOriginalFilename());
    try {
      return this.uploadImageToS3(image, folderPath, returnFileNameOnly);
    } catch (IOException e) {
      throw new S3UploadException(image.getOriginalFilename(), "파일 업로드 중 IO 오류가 발생했습니다.", e);
    }
  }

  private void validateImageFileExtention(String filename) {
    int lastDotIndex = filename.lastIndexOf(".");
    if (lastDotIndex == -1) {
      throw new S3UploadException(filename, "파일 확장자가 없습니다.");
    }

    String extention = filename.substring(lastDotIndex + 1).toLowerCase();
    List<String> allowedExtentionList = Arrays.asList("jpg", "jpeg", "png", "gif");

    if (!allowedExtentionList.contains(extention)) {
      throw new S3UploadException(filename, "지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif만 허용)");
    }
  }

  private String uploadImageToS3(MultipartFile image, String folderPath, boolean returnFileNameOnly) throws IOException {
    // null 체크
    if (amazonS3 == null) {
      throw new S3UploadException("S3 클라이언트가 초기화되지 않았습니다.");
    }
    if (bucketName == null || bucketName.isEmpty()) {
      throw new S3UploadException("S3 버킷 이름이 설정되지 않았습니다.");
    }
    
    String originalFilename = image.getOriginalFilename(); //원본 파일 명
    String extention = originalFilename.substring(originalFilename.lastIndexOf(".")); //확장자 명

    // 객실 이미지는 원본 파일명 사용 (예: hotel_deluxe.jpg)
    // 호텔 이미지는 UUID 추가
    String s3FileName = returnFileNameOnly 
        ? originalFilename  // 객실 이미지: 원본 파일명 그대로
        : UUID.randomUUID().toString().substring(0, 10) + originalFilename;  // 호텔 이미지: UUID 추가
    
    // 폴더 경로가 있으면 경로 앞에 추가
    String s3Key = folderPath != null && !folderPath.trim().isEmpty() 
        ? folderPath.trim() + "/" + s3FileName 
        : s3FileName;

    log.info("=== S3 업로드 시작 ===");
    log.info("원본 파일명: {}", originalFilename);
    log.info("S3 파일명: {}", s3FileName);
    log.info("S3 키 (경로 포함): {}", s3Key);
    log.info("폴더 경로: {}", folderPath != null ? folderPath : "루트");
    log.info("반환 형식: {}", returnFileNameOnly ? "파일명만" : "전체 URL");
    log.info("버킷 이름: {}", bucketName);
    log.info("파일 크기: {} bytes", image.getSize());
    log.info("S3 클라이언트: {}", amazonS3 != null ? "초기화됨" : "null");

    InputStream is = image.getInputStream();
    byte[] bytes = IOUtils.toByteArray(is);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType("image/" + extention.replace(".", ""));
    metadata.setContentLength(bytes.length);
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);

    try{
      PutObjectRequest putObjectRequest =
          new PutObjectRequest(bucketName, s3Key, byteArrayInputStream, metadata)
              .withCannedAcl(CannedAccessControlList.PublicRead);
      log.info("S3에 업로드 중...");
      amazonS3.putObject(putObjectRequest); // put image to S3
      log.info("✅ S3 업로드 성공!");
    }catch (Exception e){
      log.error("❌ S3 업로드 실패!");
      log.error("에러 타입: {}", e.getClass().getName());
      log.error("에러 메시지: {}", e.getMessage());
      if (e.getCause() != null) {
        log.error("원인: {}", e.getCause().getMessage());
      }
      log.error("스택 트레이스:", e);
      throw new S3UploadException(s3Key, "S3에 파일을 업로드하는 중 오류가 발생했습니다: " + e.getMessage(), e);
    }finally {
      try {
        byteArrayInputStream.close();
        is.close();
      } catch (IOException e) {
        log.warn("스트림 닫기 실패: {}", e.getMessage());
      }
    }

    if (returnFileNameOnly) {
      // 파일명만 반환 (객실 이미지)
      log.info("반환 값: {}", s3FileName);
      log.info("=== S3 업로드 완료 ===");
      return s3FileName;
    } else {
      // 전체 URL 반환 (호텔 이미지)
      try {
        String imageUrl = amazonS3.getUrl(bucketName, s3Key).toString();
        log.info("생성된 URL: {}", imageUrl);
        log.info("=== S3 업로드 완료 ===");
        return imageUrl;
      } catch (Exception e) {
        log.error("URL 생성 실패: {}", e.getMessage());
        // URL 생성 실패 시에도 파일명은 반환 (로컬 환경 대응)
        return s3FileName;
      }
    }
  }

  public void deleteImageFromS3(String imageAddress){
    String key = getKeyFromImageAddress(imageAddress);
    try{
      amazonS3.deleteObject(new DeleteObjectRequest(bucketName, key));
    }catch (Exception e){
      throw new S3DeleteException(key, "S3에서 파일을 삭제하는 중 오류가 발생했습니다.", e);
    }
  }

  private String getKeyFromImageAddress(String imageAddress){
    try{
      URL url = new URL(imageAddress);
      String decodingKey = URLDecoder.decode(url.getPath(), "UTF-8");
      return decodingKey.substring(1); // 맨 앞의 '/' 제거
    }catch (MalformedURLException | UnsupportedEncodingException e){
      throw new S3DeleteException(imageAddress, "이미지 주소를 파싱하는 중 오류가 발생했습니다.", e);
    }
  }
}