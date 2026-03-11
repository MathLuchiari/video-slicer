package com.videoslicer.video_slicer.adapters.aws;

import com.videoslicer.video_slicer.application.ports.S3StorageService;
import com.videoslicer.video_slicer.domain.video.VideoMetadata;
import net.jqwik.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AwsS3StorageAdapterPropertyTest {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String FOLDER_PREFIX = "videos/";
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
    );

    @Property(tries = 100)
    @Label("Feature: aws-s3-sqs-integration, Property 1: S3 Key Format")
    @Report(Reporting.GENERATED)
    void s3KeyFormatProperty(@ForAll("validVideoMetadata") VideoMetadata metadata) throws Exception {
        // **Validates: Requirements 1.2, 1.3, 1.5, 2.1**
        
        // Arrange
        S3Client mockS3Client = mock(S3Client.class);
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());
        
        AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(
            mockS3Client, BUCKET_NAME, FOLDER_PREFIX
        );
        
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());
        
        // Act
        String url = adapter.store(metadata, inputStream);
        
        // Assert - URL format
        assertTrue(url.startsWith("https://" + BUCKET_NAME + ".s3.amazonaws.com/"),
                  "URL should start with correct S3 URL format");
        
        // Extract key from URL
        String expectedKeyPrefix = FOLDER_PREFIX + metadata.getVideoId().toString() + "." + metadata.getExtension();
        assertTrue(url.endsWith(expectedKeyPrefix),
                  "URL should end with {prefix}/{uuid}.{extension}");
        
        // Verify S3 key format in the actual call
        verify(mockS3Client).putObject(
            argThat((PutObjectRequest request) -> {
                String key = request.key();
                
                // Key should start with folder prefix
                if (!key.startsWith(FOLDER_PREFIX)) {
                    return false;
                }
                
                // Remove prefix to get filename
                String filename = key.substring(FOLDER_PREFIX.length());
                
                // Filename should be {uuid}.{extension}
                String[] parts = filename.split("\\.");
                if (parts.length != 2) {
                    return false;
                }
                
                // First part should be a valid UUID
                if (!UUID_PATTERN.matcher(parts[0]).matches()) {
                    return false;
                }
                
                // UUID should match the metadata videoId
                if (!parts[0].equals(metadata.getVideoId().toString())) {
                    return false;
                }
                
                // Extension should match
                return parts[1].equals(metadata.getExtension());
            }),
            any(RequestBody.class)
        );
    }

    @Provide
    Arbitrary<VideoMetadata> validVideoMetadata() {
        Arbitrary<UUID> videoIds = Arbitraries.randomValue(random -> UUID.randomUUID());
        
        Arbitrary<String> fileNames = Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(1)
            .ofMaxLength(50)
            .map(name -> name + ".mp4");
        
        Arbitrary<String> extensions = Arbitraries.of("mp4", "avi", "mov", "mkv", "webm");
        
        Arbitrary<Long> sizes = Arbitraries.longs()
            .between(1L, 500L * 1024L * 1024L); // 1 byte to 500MB
        
        Arbitrary<Instant> timestamps = Arbitraries.longs()
            .between(1609459200L, 1735689600L) // 2021-01-01 to 2025-01-01
            .map(Instant::ofEpochSecond);
        
        return Combinators.combine(videoIds, fileNames, extensions, sizes, timestamps)
            .as(VideoMetadata::new);
    }

    @Property(tries = 100)
    @Label("Feature: aws-s3-sqs-integration, Property 6: S3 Retry Mechanism")
    @Report(Reporting.GENERATED)
    void s3RetryMechanismProperty(@ForAll("validVideoMetadata") VideoMetadata metadata) {
        // **Validates: Requirements 6.1**
        
        // Arrange
        S3Client mockS3Client = mock(S3Client.class);
        
        // Simulate retryable S3 exception
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
            .message("Network timeout")
            .build();
        
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(s3Exception);
        
        AwsS3StorageAdapter adapter = new AwsS3StorageAdapter(
            mockS3Client, BUCKET_NAME, FOLDER_PREFIX
        );
        
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());
        
        // Act & Assert
        long startTime = System.currentTimeMillis();
        
        try {
            adapter.store(metadata, inputStream);
            fail("Should have thrown StorageException after 3 retries");
        } catch (S3StorageService.StorageException e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Verify exactly 3 attempts were made
            verify(mockS3Client, times(3)).putObject(
                any(PutObjectRequest.class), 
                any(RequestBody.class)
            );
            
            // Verify exponential backoff occurred (minimum delay should be present)
            // Expected delays: 2^1 * 1000 = 2000ms, 2^2 * 1000 = 4000ms
            // Total expected delay: ~6000ms minimum
            // Note: We only check minimum to avoid flakiness from test execution overhead
            assertTrue(duration >= 5500, 
                      "Duration should be at least 5500ms due to exponential backoff, was: " + duration);
            
            // Verify exception message mentions 3 attempts
            assertTrue(e.getMessage().contains("3 tentativas"),
                      "Exception message should mention 3 attempts");
        }
    }
}
