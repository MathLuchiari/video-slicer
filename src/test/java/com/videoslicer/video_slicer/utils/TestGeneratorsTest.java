package com.videoslicer.video_slicer.utils;

import com.videoslicer.video_slicer.domain.video.VideoMetadata;
import net.jqwik.api.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TestGenerators to ensure all generators produce valid test data.
 */
class TestGeneratorsTest {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("mp4", "avi", "mov", "mkv", "webm");
    private static final long MAX_FILE_SIZE = 500L * 1024L * 1024L;

    @Property(tries = 50)
    @Label("validVideos() generates VideoMetadata with valid properties")
    void validVideosGeneratorProducesValidMetadata(@ForAll("validVideos") VideoMetadata metadata) {
        // Verify videoId is not null and is a valid UUID
        assertNotNull(metadata.getVideoId(), "VideoId should not be null");
        assertDoesNotThrow(() -> UUID.fromString(metadata.getVideoId().toString()),
                          "VideoId should be a valid UUID");
        
        // Verify fileName is not empty
        assertNotNull(metadata.getFileName(), "FileName should not be null");
        assertFalse(metadata.getFileName().isBlank(), "FileName should not be blank");
        
        // Verify extension is supported
        assertNotNull(metadata.getExtension(), "Extension should not be null");
        assertTrue(SUPPORTED_EXTENSIONS.contains(metadata.getExtension()),
                  "Extension should be one of the supported formats: " + metadata.getExtension());
        
        // Verify size is within valid range
        assertTrue(metadata.getSizeInBytes() > 0, "Size should be positive");
        assertTrue(metadata.getSizeInBytes() <= MAX_FILE_SIZE,
                  "Size should not exceed 500MB: " + metadata.getSizeInBytes());
        
        // Verify timestamp is not null
        assertNotNull(metadata.getUploadTimestamp(), "Upload timestamp should not be null");
        
        // Verify S3 key format
        String s3Key = metadata.getS3Key();
        assertNotNull(s3Key, "S3 key should not be null");
        assertTrue(s3Key.matches("[0-9a-f-]+\\.[a-z0-9]+"),
                  "S3 key should match UUID.extension format: " + s3Key);
    }

    @Property(tries = 50)
    @Label("invalidFiles() generates files that violate at least one validation rule")
    void invalidFilesGeneratorProducesInvalidFiles(@ForAll("invalidFiles") MultipartFile file) {
        assertNotNull(file, "File should not be null");
        assertNotNull(file.getOriginalFilename(), "Filename should not be null");
        
        // File should be invalid for at least one reason:
        // 1. Empty (size = 0)
        // 2. Unsupported extension
        // 3. Oversized (> 500MB)
        
        String filename = file.getOriginalFilename();
        long size = file.getSize();
        String extension = extractExtension(filename);
        
        boolean isEmpty = size == 0;
        boolean isUnsupportedExtension = !SUPPORTED_EXTENSIONS.contains(extension);
        boolean isOversized = size > MAX_FILE_SIZE;
        
        assertTrue(isEmpty || isUnsupportedExtension || isOversized,
                  "File should be invalid for at least one reason. " +
                  "Size: " + size + ", Extension: " + extension);
    }

    @Property(tries = 50)
    @Label("validMultipartFiles() generates valid MultipartFile instances")
    void validMultipartFilesGeneratorProducesValidFiles(@ForAll("validMultipartFiles") MultipartFile file) {
        assertNotNull(file, "File should not be null");
        
        // Verify filename
        String filename = file.getOriginalFilename();
        assertNotNull(filename, "Filename should not be null");
        assertFalse(filename.isBlank(), "Filename should not be blank");
        
        // Verify extension
        String extension = extractExtension(filename);
        assertTrue(SUPPORTED_EXTENSIONS.contains(extension),
                  "Extension should be supported: " + extension);
        
        // Verify size
        long size = file.getSize();
        assertTrue(size >= 1024, "Size should be at least 1KB: " + size);
        assertTrue(size <= MAX_FILE_SIZE, "Size should not exceed 500MB: " + size);
        
        // Verify content is not null
        assertNotNull(file.getContentType(), "Content type should not be null");
    }

    @Property(tries = 20)
    @Label("videosWithExtension() generates metadata with specified extension")
    void videosWithExtensionGeneratorUsesSpecifiedExtension(
            @ForAll("supportedExtensions") String extension) {
        
        VideoMetadata metadata = TestGenerators.videosWithExtension(extension)
            .sample();
        
        assertEquals(extension, metadata.getExtension(),
                    "Generated metadata should have the specified extension");
        
        assertTrue(metadata.getS3Key().endsWith("." + extension),
                  "S3 key should end with the specified extension");
    }

    @Property(tries = 50)
    @Label("maxSizeVideos() generates metadata at exactly 500MB")
    void maxSizeVideosGeneratorProducesMaxSizeMetadata(@ForAll("maxSizeVideos") VideoMetadata metadata) {
        assertEquals(MAX_FILE_SIZE, metadata.getSizeInBytes(),
                    "Generated metadata should have exactly 500MB size");
        
        // Verify other properties are still valid
        assertNotNull(metadata.getVideoId());
        assertNotNull(metadata.getFileName());
        assertTrue(SUPPORTED_EXTENSIONS.contains(metadata.getExtension()));
        assertNotNull(metadata.getUploadTimestamp());
    }

    // Provider methods for jqwik

    @Provide
    Arbitrary<VideoMetadata> validVideos() {
        return TestGenerators.validVideos();
    }

    @Provide
    Arbitrary<MultipartFile> invalidFiles() {
        return TestGenerators.invalidFiles();
    }

    @Provide
    Arbitrary<MultipartFile> validMultipartFiles() {
        return TestGenerators.validMultipartFiles();
    }

    @Provide
    Arbitrary<String> supportedExtensions() {
        return Arbitraries.of("mp4", "avi", "mov", "mkv", "webm");
    }

    @Provide
    Arbitrary<VideoMetadata> maxSizeVideos() {
        return TestGenerators.maxSizeVideos();
    }

    // Helper method

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }
}
