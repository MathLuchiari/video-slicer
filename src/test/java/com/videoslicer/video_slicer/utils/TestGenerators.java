package com.videoslicer.video_slicer.utils;

import com.videoslicer.video_slicer.domain.video.VideoMetadata;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

/**
 * Utility class providing custom Arbitrary generators for property-based testing.
 * This class centralizes test data generation to ensure consistency across all property tests.
 */
public class TestGenerators {

    // Supported video extensions as per requirements
    private static final String[] SUPPORTED_EXTENSIONS = {"mp4", "avi", "mov", "mkv", "webm"};
    
    // Unsupported extensions for negative testing
    private static final String[] UNSUPPORTED_EXTENSIONS = {"txt", "pdf", "doc", "exe", "zip"};
    
    // Maximum file size: 500MB
    private static final long MAX_FILE_SIZE = 500L * 1024L * 1024L;

    /**
     * Generates valid VideoMetadata objects with random but valid data.
     * All generated metadata will have:
     * - Valid UUID v4 for videoId
     * - Non-empty fileName with supported extension
     * - Supported extension (mp4, avi, mov, mkv, webm)
     * - Size between 1 byte and 500MB
     * - Valid timestamp
     * 
     * @return Arbitrary that generates valid VideoMetadata instances
     */
    public static Arbitrary<VideoMetadata> validVideos() {
        Arbitrary<UUID> videoIds = Arbitraries.randomValue(random -> UUID.randomUUID());
        
        Arbitrary<String> fileNames = Arbitraries.strings()
            .alpha()
            .numeric()
            .withChars('-', '_')
            .ofMinLength(1)
            .ofMaxLength(50)
            .map(name -> name.isEmpty() ? "video" : name)
            .map(name -> name + ".mp4"); // Add extension to filename
        
        Arbitrary<String> extensions = Arbitraries.of(SUPPORTED_EXTENSIONS);
        
        Arbitrary<Long> sizes = Arbitraries.longs()
            .between(1L, MAX_FILE_SIZE);
        
        Arbitrary<Instant> timestamps = Arbitraries.longs()
            .between(1609459200L, 1735689600L) // 2021-01-01 to 2025-01-01
            .map(Instant::ofEpochSecond);
        
        return Combinators.combine(videoIds, fileNames, extensions, sizes, timestamps)
            .as(VideoMetadata::new);
    }

    /**
     * Generates invalid file scenarios for negative testing.
     * Generates one of:
     * - Empty files (size = 0)
     * - Files with unsupported extensions
     * - Oversized files (> 500MB)
     * 
     * @return Arbitrary that generates invalid MultipartFile instances
     */
    public static Arbitrary<MultipartFile> invalidFiles() {
        return Arbitraries.oneOf(
            emptyFiles(),
            unsupportedExtensionFiles(),
            oversizedFiles()
        );
    }

    /**
     * Generates valid MultipartFile mocks for testing.
     * All generated files will have:
     * - Supported extension
     * - Size between 1KB and 500MB
     * - Non-empty content
     * - Valid filename
     * 
     * @return Arbitrary that generates valid MockMultipartFile instances
     */
    public static Arbitrary<MultipartFile> validMultipartFiles() {
        Arbitrary<String> baseNames = Arbitraries.strings()
            .alpha()
            .numeric()
            .withChars('-', '_')
            .ofMinLength(1)
            .ofMaxLength(50)
            .map(name -> name.isEmpty() ? "video" : name);
        
        Arbitrary<String> extensions = Arbitraries.of(SUPPORTED_EXTENSIONS);
        
        Arbitrary<Long> sizes = Arbitraries.longs()
            .between(1024L, MAX_FILE_SIZE); // 1KB to 500MB
        
        return Combinators.combine(baseNames, extensions, sizes)
            .as((baseName, extension, size) -> {
                String fileName = baseName + "." + extension;
                byte[] content = new byte[Math.min(size.intValue(), 10 * 1024)]; // Cap at 10KB for test performance
                return new MockMultipartFile(
                    "file",
                    fileName,
                    "video/" + extension,
                    content
                );
            });
    }

    /**
     * Generates empty files (size = 0) for validation testing.
     * 
     * @return Arbitrary that generates empty MockMultipartFile instances
     */
    private static Arbitrary<MultipartFile> emptyFiles() {
        Arbitrary<String> extensions = Arbitraries.of(SUPPORTED_EXTENSIONS);
        
        return extensions.map(ext -> {
            String fileName = "empty-video." + ext;
            return new MockMultipartFile(
                "file",
                fileName,
                "video/" + ext,
                new byte[0]
            );
        });
    }

    /**
     * Generates files with unsupported extensions for validation testing.
     * 
     * @return Arbitrary that generates MockMultipartFile instances with unsupported extensions
     */
    private static Arbitrary<MultipartFile> unsupportedExtensionFiles() {
        Arbitrary<String> baseNames = Arbitraries.strings()
            .alpha()
            .ofMinLength(1)
            .ofMaxLength(20)
            .map(name -> name.isEmpty() ? "file" : name);
        
        Arbitrary<String> extensions = Arbitraries.of(UNSUPPORTED_EXTENSIONS);
        
        return Combinators.combine(baseNames, extensions)
            .as((baseName, extension) -> {
                String fileName = baseName + "." + extension;
                byte[] content = new byte[1024]; // 1KB
                return new MockMultipartFile(
                    "file",
                    fileName,
                    "application/octet-stream",
                    content
                );
            });
    }

    /**
     * Generates oversized files (> 500MB) for validation testing.
     * Note: Only the size is set to be oversized, actual content is small for test performance.
     * 
     * @return Arbitrary that generates oversized MockMultipartFile instances
     */
    private static Arbitrary<MultipartFile> oversizedFiles() {
        Arbitrary<String> extensions = Arbitraries.of(SUPPORTED_EXTENSIONS);
        
        Arbitrary<Long> oversizedSizes = Arbitraries.longs()
            .between(MAX_FILE_SIZE + 1, MAX_FILE_SIZE * 2);
        
        return Combinators.combine(extensions, oversizedSizes)
            .as((extension, size) -> {
                String fileName = "oversized-video." + extension;
                // Create a mock that reports oversized but doesn't actually allocate that much memory
                return new MockMultipartFile(
                    "file",
                    fileName,
                    "video/" + extension,
                    new byte[1024] // Small actual content for performance
                ) {
                    @Override
                    public long getSize() {
                        return size; // Report oversized
                    }
                };
            });
    }

    /**
     * Generates VideoMetadata with specific extension for targeted testing.
     * 
     * @param extension The specific extension to use
     * @return Arbitrary that generates VideoMetadata with the specified extension
     */
    public static Arbitrary<VideoMetadata> videosWithExtension(String extension) {
        Arbitrary<UUID> videoIds = Arbitraries.randomValue(random -> UUID.randomUUID());
        
        Arbitrary<String> fileNames = Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(1)
            .ofMaxLength(50)
            .map(name -> name.isEmpty() ? "video" : name)
            .map(name -> name + "." + extension);
        
        Arbitrary<Long> sizes = Arbitraries.longs()
            .between(1L, MAX_FILE_SIZE);
        
        Arbitrary<Instant> timestamps = Arbitraries.longs()
            .between(1609459200L, 1735689600L)
            .map(Instant::ofEpochSecond);
        
        return Combinators.combine(videoIds, fileNames, Arbitraries.just(extension), sizes, timestamps)
            .as(VideoMetadata::new);
    }

    /**
     * Generates VideoMetadata with size at the boundary (exactly 500MB).
     * Useful for testing edge cases.
     * 
     * @return Arbitrary that generates VideoMetadata at max size
     */
    public static Arbitrary<VideoMetadata> maxSizeVideos() {
        Arbitrary<UUID> videoIds = Arbitraries.randomValue(random -> UUID.randomUUID());
        
        Arbitrary<String> fileNames = Arbitraries.strings()
            .alpha()
            .ofMinLength(1)
            .ofMaxLength(20)
            .map(name -> name + ".mp4");
        
        Arbitrary<String> extensions = Arbitraries.of(SUPPORTED_EXTENSIONS);
        
        Arbitrary<Instant> timestamps = Arbitraries.longs()
            .between(1609459200L, 1735689600L)
            .map(Instant::ofEpochSecond);
        
        return Combinators.combine(videoIds, fileNames, extensions, Arbitraries.just(MAX_FILE_SIZE), timestamps)
            .as(VideoMetadata::new);
    }
}
