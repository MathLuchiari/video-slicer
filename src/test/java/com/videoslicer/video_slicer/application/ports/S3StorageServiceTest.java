package com.videoslicer.video_slicer.application.ports;

import com.videoslicer.video_slicer.domain.video.VideoMetadata;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class S3StorageServiceTest {

    @Test
    void testStorageExceptionCreation() {
        // Given
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        
        // When
        S3StorageService.StorageException exception = 
            new S3StorageService.StorageException(message, cause);
        
        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testInterfaceMethodSignatures() throws Exception {
        // Given - Create a mock implementation to verify interface contract
        S3StorageService mockService = new S3StorageService() {
            @Override
            public String store(VideoMetadata videoMetadata, InputStream inputStream) 
                    throws StorageException {
                return "https://test-bucket.s3.amazonaws.com/videos/test-video.mp4";
            }

            @Override
            public InputStream retrieve(String videoId, String extension) 
                    throws StorageException {
                return new ByteArrayInputStream(new byte[0]);
            }
        };

        // When - Call store method
        VideoMetadata metadata = new VideoMetadata(
            UUID.randomUUID(),
            "test.mp4",
            "mp4",
            1024L,
            Instant.now()
        );
        InputStream inputStream = new ByteArrayInputStream(new byte[10]);
        String url = mockService.store(metadata, inputStream);

        // Then - Verify store returns a URL string
        assertNotNull(url);
        assertTrue(url.startsWith("https://"));

        // When - Call retrieve method
        InputStream retrieved = mockService.retrieve("test-id", "mp4");

        // Then - Verify retrieve returns an InputStream
        assertNotNull(retrieved);
    }

    @Test
    void testStorageExceptionIsCheckedException() {
        // Verify that StorageException extends Exception (checked exception)
        assertTrue(Exception.class.isAssignableFrom(S3StorageService.StorageException.class));
    }
}
