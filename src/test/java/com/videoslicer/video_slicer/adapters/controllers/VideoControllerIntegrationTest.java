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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoControllerIntegrationTest {

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
    void testUploadWithValidFile_Returns201() throws Exception {
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
        assertTrue(body.containsKey("message"));
        assertTrue(body.containsKey("content"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) body.get("content");
        assertTrue(content.containsKey("fileName"));
        assertTrue(content.containsKey("videoId"));
        assertTrue(content.containsKey("videoUrl"));
        assertEquals(expectedUrl, content.get("videoUrl"));
    }

    @Test
    void testUploadWithInvalidExtension_Returns400() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-video.txt",
            "text/plain",
            "test content".getBytes()
        );

        // Act
        ResponseEntity<?> response = videoController.upload(file);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.containsKey("message"));
        String message = (String) body.get("message");
        assertTrue(message.contains("não suportada"));
    }

    @Test
    void testUploadWithEmptyFile_Returns400() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-video.mp4",
            "video/mp4",
            new byte[0]
        );

        // Act
        ResponseEntity<?> response = videoController.upload(file);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.containsKey("message"));
        String message = (String) body.get("message");
        assertTrue(message.contains("vazio"));
    }

    @Test
    void testUploadWithS3Failure_Returns500() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-video.mp4",
            "video/mp4",
            "test video content".getBytes()
        );

        when(s3StorageService.store(any(), any()))
            .thenThrow(new S3StorageService.StorageException("S3 connection failed", new RuntimeException()));

        // Act
        ResponseEntity<?> response = videoController.upload(file);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.containsKey("message"));
    }

    @Test
    void testUploadResponseStructure_ContainsAllRequiredFields() throws Exception {
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
        
        // Verify message field
        assertTrue(body.containsKey("message"));
        assertInstanceOf(String.class, body.get("message"));
        
        // Verify content field
        assertTrue(body.containsKey("content"));
        assertInstanceOf(Map.class, body.get("content"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) body.get("content");
        
        // Verify all required fields in content
        assertTrue(content.containsKey("fileName"));
        assertInstanceOf(String.class, content.get("fileName"));
        
        assertTrue(content.containsKey("videoId"));
        assertInstanceOf(String.class, content.get("videoId"));
        
        assertTrue(content.containsKey("videoUrl"));
        assertInstanceOf(String.class, content.get("videoUrl"));
    }
}
