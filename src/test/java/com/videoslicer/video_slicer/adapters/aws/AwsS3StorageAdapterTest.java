package com.videoslicer.video_slicer.adapters.aws;

import com.videoslicer.video_slicer.application.ports.S3StorageService;
import com.videoslicer.video_slicer.domain.video.VideoMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsS3StorageAdapterTest {

    @Mock
    private S3Client s3Client;

    private AwsS3StorageAdapter adapter;
    
    private static final String BUCKET_NAME = "test-bucket";
    private static final String FOLDER_PREFIX = "videos/";

    @BeforeEach
    void setUp() {
        adapter = new AwsS3StorageAdapter(s3Client, BUCKET_NAME, FOLDER_PREFIX);
    }

    @Test
    void testStoreSuccessReturnsCorrectUrl() throws Exception {
        // Arrange
        UUID videoId = UUID.randomUUID();
        VideoMetadata metadata = new VideoMetadata(
            videoId, "test.mp4", "mp4", 1024L, Instant.now()
        );
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());
        
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        // Act
        String url = adapter.store(metadata, inputStream);

        // Assert
        String expectedUrl = String.format("https://%s.s3.amazonaws.com/%s%s.mp4", 
                                          BUCKET_NAME, FOLDER_PREFIX, videoId);
        assertEquals(expectedUrl, url);
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testStoreFailsAfter3RetriesThrowsStorageException() {
        // Arrange
        UUID videoId = UUID.randomUUID();
        VideoMetadata metadata = new VideoMetadata(
            videoId, "test.mp4", "mp4", 1024L, Instant.now()
        );
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());
        
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
            .message("Network timeout")
            .build();
        
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(s3Exception);

        // Act & Assert
        S3StorageService.StorageException exception = assertThrows(
            S3StorageService.StorageException.class,
            () -> adapter.store(metadata, inputStream)
        );
        
        assertTrue(exception.getMessage().contains("3 tentativas"));
        verify(s3Client, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testStoreSucceedsOnSecondAttemptAfterFirstFailure() throws Exception {
        // Arrange
        UUID videoId = UUID.randomUUID();
        VideoMetadata metadata = new VideoMetadata(
            videoId, "test.mp4", "mp4", 1024L, Instant.now()
        );
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());
        
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
            .message("Temporary failure")
            .build();
        
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(s3Exception)
            .thenReturn(PutObjectResponse.builder().build());

        // Act
        String url = adapter.store(metadata, inputStream);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(videoId.toString()));
        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testRetrieveExistingVideoReturnsInputStream() throws Exception {
        // Arrange
        String videoId = UUID.randomUUID().toString();
        String extension = "mp4";
        byte[] videoData = "video content".getBytes();
        
        ResponseInputStream<GetObjectResponse> mockResponse = 
            new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream(videoData)
            );
        
        when(s3Client.getObject(any(GetObjectRequest.class)))
            .thenReturn(mockResponse);

        // Act
        InputStream result = adapter.retrieve(videoId, extension);

        // Assert
        assertNotNull(result);
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    void testRetrieveNonExistingVideoThrowsStorageException() {
        // Arrange
        String videoId = UUID.randomUUID().toString();
        String extension = "mp4";
        
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
            .message("NoSuchKey")
            .build();
        
        when(s3Client.getObject(any(GetObjectRequest.class)))
            .thenThrow(s3Exception);

        // Act & Assert
        S3StorageService.StorageException exception = assertThrows(
            S3StorageService.StorageException.class,
            () -> adapter.retrieve(videoId, extension)
        );
        
        assertTrue(exception.getMessage().contains("Erro ao recuperar vídeo do S3"));
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    void testStoreUsesCorrectContentTypeForMp4() throws Exception {
        // Arrange
        UUID videoId = UUID.randomUUID();
        VideoMetadata metadata = new VideoMetadata(
            videoId, "test.mp4", "mp4", 1024L, Instant.now()
        );
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());
        
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        // Act
        adapter.store(metadata, inputStream);

        // Assert
        verify(s3Client).putObject(
            argThat((PutObjectRequest request) -> request.contentType().equals("video/mp4")), 
            any(RequestBody.class)
        );
    }

    @Test
    void testStoreUsesCorrectContentTypeForAvi() throws Exception {
        // Arrange
        UUID videoId = UUID.randomUUID();
        VideoMetadata metadata = new VideoMetadata(
            videoId, "test.avi", "avi", 1024L, Instant.now()
        );
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());
        
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        // Act
        adapter.store(metadata, inputStream);

        // Assert
        verify(s3Client).putObject(
            argThat((PutObjectRequest request) -> request.contentType().equals("video/x-msvideo")), 
            any(RequestBody.class)
        );
    }
}
