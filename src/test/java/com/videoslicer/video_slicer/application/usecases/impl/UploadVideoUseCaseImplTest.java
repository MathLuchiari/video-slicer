package com.videoslicer.video_slicer.application.usecases.impl;

import com.videoslicer.video_slicer.application.ports.S3StorageService;
import com.videoslicer.video_slicer.application.ports.SQSPublisherService;
import com.videoslicer.video_slicer.application.usecases.UploadVideoUseCase.UploadResult;
import com.videoslicer.video_slicer.domain.video.VideoMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadVideoUseCaseImplTest {

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private SQSPublisherService sqsPublisherService;

    private UploadVideoUseCaseImpl uploadVideoUseCase;

    @BeforeEach
    void setUp() {
        uploadVideoUseCase = new UploadVideoUseCaseImpl(s3StorageService, sqsPublisherService);
    }

    @Test
    void testUploadValidVideoReturnsSuccessWithVideoIdAndUrl() throws Exception {
        // Arrange
        byte[] content = new byte[1024]; // 1KB
        MultipartFile file = new MockMultipartFile(
            "file",
            "test-video.mp4",
            "video/mp4",
            content
        );
        
        String expectedUrl = "https://my-bucket.s3.amazonaws.com/videos/test-uuid.mp4";
        when(s3StorageService.store(any(VideoMetadata.class), any(InputStream.class)))
            .thenReturn(expectedUrl);
        
        // Act
        UploadResult result = uploadVideoUseCase.execute(file);
        
        // Assert
        assertTrue(result.success);
        assertNotNull(result.videoId);
        assertEquals(expectedUrl, result.videoUrl);
        assertNotNull(result.fileName);
        assertTrue(result.fileName.endsWith(".mp4"));
        assertEquals("Vídeo carregado com sucesso!", result.message);
        
        verify(s3StorageService, times(1)).store(any(VideoMetadata.class), any(InputStream.class));
        verify(sqsPublisherService, times(1)).publish(any(VideoMetadata.class));
    }

    @Test
    void testUploadEmptyFileReturnsFalse() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "empty.mp4",
            "video/mp4",
            new byte[0]
        );
        
        // Act
        UploadResult result = uploadVideoUseCase.execute(file);
        
        // Assert
        assertFalse(result.success);
        assertNull(result.videoId);
        assertNull(result.videoUrl);
        assertEquals("Arquivo vazio", result.message);
        
        verifyNoInteractions(s3StorageService);
        verifyNoInteractions(sqsPublisherService);
    }

    @Test
    void testS3FailureReturnsFalse() throws Exception {
        // Arrange
        byte[] content = new byte[1024];
        MultipartFile file = new MockMultipartFile(
            "file",
            "test-video.mp4",
            "video/mp4",
            content
        );
        
        when(s3StorageService.store(any(VideoMetadata.class), any(InputStream.class)))
            .thenAnswer(invocation -> {
                throw new S3StorageService.StorageException("S3 connection failed", new RuntimeException());
            });
        
        // Act
        UploadResult result = uploadVideoUseCase.execute(file);
        
        // Assert
        assertFalse(result.success);
        assertNull(result.videoId);
        assertNull(result.videoUrl);
        assertTrue(result.message.contains("Erro ao carregar o arquivo"));
        
        verify(s3StorageService, times(1)).store(any(VideoMetadata.class), any(InputStream.class));
        verifyNoInteractions(sqsPublisherService);
    }

    @Test
    void testSQSFailureDoesNotPreventUploadSuccess() throws Exception {
        // Arrange
        byte[] content = new byte[1024];
        MultipartFile file = new MockMultipartFile(
            "file",
            "test-video.mp4",
            "video/mp4",
            content
        );
        
        String expectedUrl = "https://my-bucket.s3.amazonaws.com/videos/test-uuid.mp4";
        when(s3StorageService.store(any(VideoMetadata.class), any(InputStream.class)))
            .thenReturn(expectedUrl);
        
        doAnswer(invocation -> {
            throw new SQSPublisherService.PublishException("SQS unavailable", new RuntimeException());
        }).when(sqsPublisherService).publish(any(VideoMetadata.class));
        
        // Act
        UploadResult result = uploadVideoUseCase.execute(file);
        
        // Assert
        assertTrue(result.success, "Upload should succeed even if SQS fails");
        assertNotNull(result.videoId);
        assertEquals(expectedUrl, result.videoUrl);
        assertEquals("Vídeo carregado com sucesso!", result.message);
        
        verify(s3StorageService, times(1)).store(any(VideoMetadata.class), any(InputStream.class));
        verify(sqsPublisherService, times(1)).publish(any(VideoMetadata.class));
    }
}
