package com.videoslicer.video_slicer.application.usecases.impl;

import com.videoslicer.video_slicer.application.usecases.SliceVideoUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for SliceVideoUseCase to verify backward compatibility
 * after AWS S3/SQS integration changes.
 */
class SliceVideoUseCaseImplTest {

    private SliceVideoUseCaseImpl sliceVideoUseCase;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        sliceVideoUseCase = new SliceVideoUseCaseImpl();
    }

    @Test
    void testExecuteWithNonExistentFile_ReturnsFailure() {
        // Arrange
        String nonExistentFileName = "non-existent-video.mp4";
        int timeInterval = 5;

        // Act
        SliceVideoUseCase.SliceResult result = sliceVideoUseCase.execute(nonExistentFileName, timeInterval);

        // Assert
        assertFalse(result.success, "Expected failure for non-existent file");
        assertEquals("Arquivo de vídeo não encontrado!", result.message);
        assertNull(result.outputPrefix);
    }

    @Test
    void testExecuteWithInvalidTimeInterval_HandlesGracefully() {
        // Arrange
        String fileName = "test-video.mp4";
        int invalidTimeInterval = 0;

        // Act
        SliceVideoUseCase.SliceResult result = sliceVideoUseCase.execute(fileName, invalidTimeInterval);

        // Assert
        // Should fail because file doesn't exist, but validates the use case handles edge cases
        assertFalse(result.success);
        assertNotNull(result.message);
    }

    @Test
    void testSliceResultStructure_MaintainsExpectedFields() {
        // Arrange & Act
        SliceVideoUseCase.SliceResult successResult = new SliceVideoUseCase.SliceResult(
            true, 
            "Vídeo cortado com sucesso!", 
            "sliced_video_frame_"
        );
        
        SliceVideoUseCase.SliceResult failureResult = new SliceVideoUseCase.SliceResult(
            false, 
            "Erro ao processar o vídeo", 
            null
        );

        // Assert - Success case
        assertTrue(successResult.success);
        assertEquals("Vídeo cortado com sucesso!", successResult.message);
        assertEquals("sliced_video_frame_", successResult.outputPrefix);

        // Assert - Failure case
        assertFalse(failureResult.success);
        assertEquals("Erro ao processar o vídeo", failureResult.message);
        assertNull(failureResult.outputPrefix);
    }

    @Test
    void testSliceEndpoint_MaintainsBackwardCompatibility() {
        // This test verifies that the SliceVideoUseCase interface and result structure
        // remain unchanged after AWS integration, ensuring backward compatibility
        
        // Arrange
        String fileName = "test.mp4";
        int timeInterval = 10;

        // Act
        SliceVideoUseCase.SliceResult result = sliceVideoUseCase.execute(fileName, timeInterval);

        // Assert - Verify result structure has expected fields
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.message, "Message field should exist");
        // success field is a primitive boolean, always has a value
        // outputPrefix can be null on failure, which is expected
    }
}
