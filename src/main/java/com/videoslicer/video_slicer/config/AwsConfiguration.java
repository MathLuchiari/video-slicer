package com.videoslicer.video_slicer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfiguration {
    
    @Value("${aws.region}")
    private String region;
    
    @Value("${aws.access-key-id:#{null}}")
    private String accessKeyId;
    
    @Value("${aws.secret-access-key:#{null}}")
    private String secretAccessKey;
    
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        // Prioriza credenciais explícitas, senão usa DefaultCredentialsProvider
        // que busca em variáveis de ambiente, arquivo de credenciais, etc.
        if (accessKeyId != null && secretAccessKey != null) {
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            );
        }
        return DefaultCredentialsProvider.create();
    }
    
    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build();
    }
    
    @Bean
    public SqsClient sqsClient(AwsCredentialsProvider credentialsProvider) {
        return SqsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build();
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
