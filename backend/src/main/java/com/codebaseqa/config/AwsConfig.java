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
        
        SqsClient client;
        
        // Check if credentials are provided (local development)
        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            log.info("   Using static credentials (local development)");
            log.info("   Access Key: {}****", accessKey.substring(0, Math.min(4, accessKey.length())));
            
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            client = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
        } else {
            // Use default credential provider chain (EC2 IAM role, environment variables, etc.)
            log.info("   Using default credential provider chain (EC2 IAM role or environment)");
            client = SqsClient.builder()
                .region(Region.of(region))
                .build();
        }
        
        log.info("✅ AWS SQS Client initialized successfully");
        return client;
    }
}
