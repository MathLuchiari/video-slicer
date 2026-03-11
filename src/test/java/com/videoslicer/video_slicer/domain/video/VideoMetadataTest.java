package com.videoslicer.video_slicer.domain.video;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VideoMetadata class
 * Validates: Requirements 1.2, 1.3
 */
class VideoMetadataTest {

    @Test
    void testConstructorWithValidValues() {
        // Arrange
        UUID videoId = UUID.randomUUID();
        String fileName = "test-video.mp4";
        String extension = "mp4";
        long sizeInBytes = 1024000L;
        Instant uploadTimestamp = Instant.now();

        // Act
        VideoMetadata metadata = new VideoMetadata(
            videoId, 
            fileName, 
            extension, 
            sizeInBytes, 
            uploadTimestamp
        );

        // Assert
        assertNotNull(metadata);
        assertEquals(videoId, metadata.getVideoId());
        assertEquals(fileName, metadata.getFileName());
        assertEquals(extension, metadata.getExtension());
        assertEquals(sizeInBytes, metadata.getSizeInBytes());
        assertEquals(uploadTimestamp, metadata.getUploadTimestamp());
    }

    @Test
    void testGetS3KeyReturnsCorrectFormat() {
        // Arrange
        UUID videoId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String extension = "mp4";
        VideoMetadata metadata = new VideoMetadata(
            videoId,
            "original-name.mp4",
            extension,
            1024000L,
            Instant.now()
        );

        // Act
        String s3Key = metadata.getS3Key();

        // Assert
        assertEquals("123e4567-e89b-12d3-a456-426614174000.mp4", s3Key);
        assertTrue(s3Key.contains(videoId.toString()));
        assertTrue(s3Key.endsWith("." + extension));
    }

    @Test
    void testGetS3KeyWithDifferentExtensions() {
        // Arrange
        UUID videoId = UUID.randomUUID();
        String[] extensions = {"mp4", "avi", "mov", "mkv", "webm"};

        for (String extension : extensions) {
            // Act
            VideoMetadata metadata = new VideoMetadata(
                videoId,
                "test." + extension,
                extension,
                1024000L,
                Instant.now()
            );
            String s3Key = metadata.getS3Key();

            // Assert
            assertEquals(videoId.toString() + "." + extension, s3Key);
        }
    }

    @Test
    void testVideoMetadataImmutability() {
        // Arrange
        UUID videoId = UUID.randomUUID();
        String fileName = "test.mp4";
        String extension = "mp4";
        long sizeInBytes = 1024000L;
        Instant uploadTimestamp = Instant.now();

        // Act
        VideoMetadata metadata = new VideoMetadata(
            videoId, 
            fileName, 
            extension, 
            sizeInBytes, 
            uploadTimestamp
        );

        // Assert - verify all getters return the same values
        assertEquals(videoId, metadata.getVideoId());
        assertEquals(fileName, metadata.getFileName());
        assertEquals(extension, metadata.getExtension());
        assertEquals(sizeInBytes, metadata.getSizeInBytes());
        assertEquals(uploadTimestamp, metadata.getUploadTimestamp());
        
        // Calling getters multiple times should return same values
        assertEquals(metadata.getVideoId(), metadata.getVideoId());
        assertEquals(metadata.getS3Key(), metadata.getS3Key());
    }

    @Test
    void testVideoMetadataWithLargeFileSize() {
        // Arrange
        UUID videoId = UUID.randomUUID();
        long largeSize = 500L * 1024 * 1024; // 500MB

        // Act
        VideoMetadata metadata = new VideoMetadata(
            videoId,
            "large-video.mp4",
            "mp4",
            largeSize,
            Instant.now()
        );

        // Assert
        assertEquals(largeSize, metadata.getSizeInBytes());
    }

    @Test
    void testVideoMetadataWithMinimalFileSize() {
        // Arrange
        UUID videoId = UUID.randomUUID();
        long minimalSize = 1L;

        // Act
        VideoMetadata metadata = new VideoMetadata(
            videoId,
            "tiny-video.mp4",
            "mp4",
            minimalSize,
            Instant.now()
        );

        // Assert
        assertEquals(minimalSize, metadata.getSizeInBytes());
    }
}
