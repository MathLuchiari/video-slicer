package com.videoslicer.video_slicer.adapters.controllers;

import com.videoslicer.video_slicer.application.ports.S3StorageService;
import com.videoslicer.video_slicer.application.ports.SQSPublisherService;
import com.videoslicer.video_slicer.application.usecases.UploadVideoUseCase;
import com.videoslicer.video_slicer.application.usecases.impl.UploadVideoUseCaseImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Backward compatibility tests for AWS S3/SQS integration.
 * Validates that existing functionality remains intact after integration changes.
 * 
 * Requirements: 9.3, 9.4 - Maintain backward compatibility with fileName field
 */
@ExtendWith(MockitoExtension.class)
class BackwardCompatibilityTest {

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private SQSPublisherService sqsPublisherService;

    private VideoController videoController;
    private UploadVideoUseCase uploadUseCase;

    @BeforeEach
    void setUp() {
        uploadUseCase = new UploadVideoUseCaseImpl(s3StorageService, sqsPublisherService);
        videoController = new VideoController(uploadUseCase, null);
    }

    @Test
    void testUploadResponse_ContainsFileNameField() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-video.mp4",
            "video/mp4",
            "test video content".getBytes()
        );

        String expectedUrl = "https://test-bucket.s3.amazonaws.com/videos/test-id.mp4";
        when(s3StorageService.store(any(), any())).thenReturn(expectedUrl);

        // Act
        ResponseEntity<?> response = videoController.upload(file);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.containsKey("content"), "Response should contain 'content' field");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) body.get("content");
        
        // Verify fileName field exists (backward compatibility)
        assertTrue(content.containsKey("fileName"), 
            "Response content must contain 'fileName' field for backward compatibility");
        assertNotNull(content.get("fileName"), 
            "fileName field must not be null");
    }

    @Test
    void testUploadResponse_FileNameFollowsExpectedFormat() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "my-video.mp4",
            "video/mp4",
            "test video content".getBytes()
        );

        String expectedUrl = "https://test-bucket.s3.amazonaws.com/videos/550e8400-e29b-41d4-a716-446655440000.mp4";
        when(s3StorageService.store(any(), any())).thenReturn(expectedUrl);

        // Act
        ResponseEntity<?> response = videoController.upload(file);

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) body.get("content");
        
        String fileName = (String) content.get("fileName");
        
        // Verify fileName format: {uuid}.{extension}
        Pattern uuidExtensionPattern = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(mp4|avi|mov|mkv|webm)$"
        );
        assertTrue(uuidExtensionPattern.matcher(fileName).matches(),
            "fileName should follow format {uuid}.{extension}, got: " + fileName);
    }

    @Test
    void testUploadResponse_FileNameMatchesVideoId() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.mp4",
            "video/mp4",
            "test video content".getBytes()
        );

        String expectedUrl = "https://test-bucket.s3.amazonaws.com/videos/test-id.mp4";
        when(s3StorageService.store(any(), any())).thenReturn(expectedUrl);

        // Act
        ResponseEntity<?> response = videoController.upload(file);

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) body.get("content");
        
        String fileName = (String) content.get("fileName");
        String videoId = (String) content.get("videoId");
        
        // Extract UUID from fileName (before the extension)
        String fileNameUuid = fileName.substring(0, fileName.lastIndexOf('.'));
        
        // Verify videoId matches the UUID in fileName
        assertEquals(videoId, fileNameUuid,
            "videoId should match the UUID portion of fileName");
    }

    @Test
    void testUploadResponse_FileNamePreservesExtension() throws Exception {
        // Test with different video extensions
        String[] extensions = {"mp4", "avi", "mov", "mkv", "webm"};
        
        for (String extension : extensions) {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-video." + extension,
                "video/" + extension,
                "test video content".getBytes()
            );

            String expectedUrl = "https://test-bucket.s3.amazonaws.com/videos/test-id." + extension;
            when(s3StorageService.store(any(), any())).thenReturn(expectedUrl);

            // Act
            ResponseEntity<?> response = videoController.upload(file);

            // Assert
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) body.get("content");
            
            String fileName = (String) content.get("fileName");
            
            // Verify fileName ends with the correct extension
            assertTrue(fileName.endsWith("." + extension),
                "fileName should preserve original extension ." + extension + ", got: " + fileName);
        }
    }

    @Test
    void testUploadResponse_AllFieldsCoexist() throws Exception {
        // Verify that new fields (videoId, videoUrl) coexist with legacy field (fileName)
        
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-video.mp4",
            "video/mp4",
            "test video content".getBytes()
        );

        String expectedUrl = "https://test-bucket.s3.amazonaws.com/videos/test-id.mp4";
        when(s3StorageService.store(any(), any())).thenReturn(expectedUrl);

        // Act
        ResponseEntity<?> response = videoController.upload(file);

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) body.get("content");
        
        // Verify all three fields exist together
        assertTrue(content.containsKey("fileName"), "Legacy field 'fileName' must exist");
        assertTrue(content.containsKey("videoId"), "New field 'videoId' must exist");
        assertTrue(content.containsKey("videoUrl"), "New field 'videoUrl' must exist");
        
        // Verify all fields have non-null values
        assertNotNull(content.get("fileName"));
        assertNotNull(content.get("videoId"));
        assertNotNull(content.get("videoUrl"));
    }
}
