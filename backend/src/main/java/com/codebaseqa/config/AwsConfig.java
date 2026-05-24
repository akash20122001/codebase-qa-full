package com.codebaseqa.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@Slf4j
public class AwsConfig {

    @Value("${app.aws.region}")
    private String region;

    @Value("${app.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${app.aws.credentials.secret-key:}")
    private String secretKey;

    @Bean
    public SqsClient sqsClient() {
        log.info("🔧 Initializing AWS SQS Client...");
        log.info("   Region: {}", region);
        log.info("   Access Key: {}", accessKey != null && !accessKey.isEmpty() ? accessKey.substring(0, 4) + "****" : "NOT SET");
        log.info("   Secret Key: {}", secretKey != null && !secretKey.isEmpty() ? "****" : "NOT SET");
        
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            log.error("❌ AWS credentials not configured!");
            log.error("   Please ensure AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY are set in backend/.env file");
            throw new IllegalStateException(
                "AWS credentials not configured. Please set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY in your .env file"
            );
        }

        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

        SqsClient client = SqsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .build();
        
        log.info("✅ AWS SQS Client initialized successfully");
        return client;
    }
}
