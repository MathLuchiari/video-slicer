package com.videoslicer.video_slicer.domain.video;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VideoValidation class
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4
 */
class VideoValidationTest {

    @Test
    void testValidateWithValidFile() {
        // Arrange
        String fileName = "video.mp4";
        long fileSize = 100 * 1024 * 1024; // 100MB

        // Act
        VideoValidation.ValidationResult result = VideoValidation.validate(fileName, fileSize);

        // Assert
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"video.mp4", "video.avi", "video.mov", "video.mkv", "video.webm"})
    void testValidateWithSupportedExtensions(String fileName) {
        // Arrange
        long fileSize = 100 * 1024 * 1024; // 100MB

        // Act
        VideoValidation.ValidationResult result = VideoValidation.validate(fileName, fileSize);

        // Assert
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIDEO.MP4", "Video.AVI", "VIDEO.MOV", "video.MKV", "ViDeO.WEBM"})
    void testValidateWithMixedCaseExtensions(String fileName) {
        // Arrange
        long fileSize = 100 * 1024 * 1024; // 100MB

        // Act
        VideoValidation.ValidationResult result = VideoValidation.validate(fileName, fileSize);

        // Assert
        assertTrue(result.isValid(), "Should accept mixed case extensions");
        assertNull(result.getErrorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"video.txt", "video.pdf", "video.jpg", "video.doc"})
    void testValidateWithUnsupportedExtensions(String fileName) {
        // Arrange
        long fileSize = 100 * 1024 * 1024; // 100MB

        // Act
        VideoValidation.ValidationResult result = VideoValidation.validate(fileName, fileSize);

        // Assert
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Extensão não suportada"));
    }

    @Test
    void testValidateWithNullFileName() {
        // Arrange
        String fileName = null;
        long fileSize = 100 * 1024 * 1024;

        // Act
        VideoValidation.ValidationResult result = VideoValidation.validate(fileName, fileSize);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Nome do arquivo não pode ser vazio", result.getErrorMessage());
    }

    @Test
    void testValidateWithBlankFileName() {
        // Arrange
        String fileName = "   ";
        long fileSize = 100 * 1024 * 1024;

        // Act
        VideoValidation.ValidationResult result = VideoValidation.validate(fileName, fileSize);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Nome do arquivo não pode ser vazio", result.getErrorMessage());
    }

    @Test
    void testValidateWithEmptyFileName() {
        // Arrange
        String fileName = "";
        long fileSize = 100 * 1024 * 1024;

        // Act
        VideoValidation.ValidationResult result = VideoValidation.validate(fileName, fileSize);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Nome do arquivo não pode ser vazio", result.getErrorMessage());
    }

    @Test
    void testValidateWithZeroFileSize() {
        // Arrange
        String fileName = "video.mp4";
        long fileSize = 0;

        // Act
        VideoValidation.ValidationResult result = VideoValidation.validate(fileName, fileSize);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Arquivo vazio", result.getErrorMessage());
    }

    @Test
    void testValidateWithNegativeFileSize() {
        // Arrange
        String fileName = "video.mp4";
        long fileSize = -1;

        // Act
        VideoValidation.ValidationResult result = VideoValidation.validate(fileName, fileSize);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Arquivo vazio", result.getErrorMessage());
    }

    @Test
    void testValidateWithMaxAllowedFileSize() {
        // Arrange
        String fileName = "video.mp4";
        long fileSize = 500 * 1024 * 1024; // Exactly 500MB

        // Act
        VideoValidation.ValidationResult result = VideoValidation.validate(fileName, fileSize);

        // Assert
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void testValidateWithFileSizeExceedingLimit() {
        // Arrange
        String fileName = "video.mp4";
        long fileSize = (500 * 1024 * 1024) + 1; // 500MB + 1 byte

        // Act
        VideoValidation.ValidationResult result = VideoValidation.validate(fileName, fileSize);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("excede o tamanho máximo de 500MB"));
    }

    @Test
    void testValidateWithFileNameWithoutExtension() {
        // Arrange
        String fileName = "videofile";
        long fileSize = 100 * 1024 * 1024;

        // Act
        VideoValidation.ValidationResult result = VideoValidation.validate(fileName, fileSize);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Extensão não suportada"));
    }

    @Test
    void testValidationResultValid() {
        // Act
        VideoValidation.ValidationResult result = VideoValidation.ValidationResult.valid();

        // Assert
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void testValidationResultInvalid() {
        // Arrange
        String errorMessage = "Test error message";

        // Act
        VideoValidation.ValidationResult result = VideoValidation.ValidationResult.invalid(errorMessage);

        // Assert
        assertFalse(result.isValid());
        assertEquals(errorMessage, result.getErrorMessage());
    }
}
