package com.sist.backend.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {
  private static final Logger logger = LoggerFactory.getLogger(S3Config.class);

  @Value("${cloud.aws.credentials.accessKey}")
  private String accessKey;
  @Value("${cloud.aws.credentials.secretKey}")
  private String secretKey;
  @Value("${cloud.aws.region.static}")
  private String region;
  @Value("${cloud.aws.s3.bucketName}")
  private String bucketName;

  @Bean
  public AmazonS3 amazonS3() {
    logger.info("=================================================");
    logger.info("AWS S3 Configuration Loading");
    logger.info("Region: {}", region);
    logger.info("Bucket Name: {}", bucketName);
    logger.info("Access Key: {}...", accessKey != null && accessKey.length() > 4 ? accessKey.substring(0, 4) : "null");
    logger.info("Secret Key: {}", secretKey != null ? "***설정됨***" : "null");
    logger.info("=================================================");

    AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

    return AmazonS3ClientBuilder
        .standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withRegion(region)
        .build();
  }

}