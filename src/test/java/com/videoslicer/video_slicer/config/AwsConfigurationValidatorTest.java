package com.videoslicer.video_slicer.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AwsConfigurationValidator class
 * Validates: Requirements 4.5, 5.5
 */
class AwsConfigurationValidatorTest {

    @Test
    void testValidateConfigurationFailsWithoutRegion() {
        // Arrange
        AwsConfigurationValidator validator = new AwsConfigurationValidator();
        ReflectionTestUtils.setField(validator, "region", null);
        ReflectionTestUtils.setField(validator, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(validator, "queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/test-queue");

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            validator::validateConfiguration);
        
        assertTrue(exception.getMessage().contains("Configurações AWS obrigatórias ausentes"));
        assertTrue(exception.getMessage().contains("aws.region não configurado"));
    }

    @Test
    void testValidateConfigurationFailsWithBlankRegion() {
        // Arrange
        AwsConfigurationValidator validator = new AwsConfigurationValidator();
        ReflectionTestUtils.setField(validator, "region", "   ");
        ReflectionTestUtils.setField(validator, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(validator, "queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/test-queue");

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            validator::validateConfiguration);
        
        assertTrue(exception.getMessage().contains("Configurações AWS obrigatórias ausentes"));
        assertTrue(exception.getMessage().contains("aws.region não configurado"));
    }

    @Test
    void testValidateConfigurationFailsWithoutBucketName() {
        // Arrange
        AwsConfigurationValidator validator = new AwsConfigurationValidator();
        ReflectionTestUtils.setField(validator, "region", "us-east-1");
        ReflectionTestUtils.setField(validator, "bucketName", null);
        ReflectionTestUtils.setField(validator, "queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/test-queue");

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            validator::validateConfiguration);
        
        assertTrue(exception.getMessage().contains("Configurações AWS obrigatórias ausentes"));
        assertTrue(exception.getMessage().contains("aws.s3.bucket-name não configurado"));
    }

    @Test
    void testValidateConfigurationFailsWithBlankBucketName() {
        // Arrange
        AwsConfigurationValidator validator = new AwsConfigurationValidator();
        ReflectionTestUtils.setField(validator, "region", "us-east-1");
        ReflectionTestUtils.setField(validator, "bucketName", "");
        ReflectionTestUtils.setField(validator, "queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/test-queue");

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            validator::validateConfiguration);
        
        assertTrue(exception.getMessage().contains("Configurações AWS obrigatórias ausentes"));
        assertTrue(exception.getMessage().contains("aws.s3.bucket-name não configurado"));
    }

    @Test
    void testValidateConfigurationFailsWithoutQueueUrl() {
        // Arrange
        AwsConfigurationValidator validator = new AwsConfigurationValidator();
        ReflectionTestUtils.setField(validator, "region", "us-east-1");
        ReflectionTestUtils.setField(validator, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(validator, "queueUrl", null);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            validator::validateConfiguration);
        
        assertTrue(exception.getMessage().contains("Configurações AWS obrigatórias ausentes"));
        assertTrue(exception.getMessage().contains("aws.sqs.queue-url não configurado"));
    }

    @Test
    void testValidateConfigurationFailsWithBlankQueueUrl() {
        // Arrange
        AwsConfigurationValidator validator = new AwsConfigurationValidator();
        ReflectionTestUtils.setField(validator, "region", "us-east-1");
        ReflectionTestUtils.setField(validator, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(validator, "queueUrl", "  ");

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            validator::validateConfiguration);
        
        assertTrue(exception.getMessage().contains("Configurações AWS obrigatórias ausentes"));
        assertTrue(exception.getMessage().contains("aws.sqs.queue-url não configurado"));
    }

    @Test
    void testValidateConfigurationFailsWithMultipleMissingConfigurations() {
        // Arrange
        AwsConfigurationValidator validator = new AwsConfigurationValidator();
        ReflectionTestUtils.setField(validator, "region", null);
        ReflectionTestUtils.setField(validator, "bucketName", null);
        ReflectionTestUtils.setField(validator, "queueUrl", null);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            validator::validateConfiguration);
        
        assertTrue(exception.getMessage().contains("Configurações AWS obrigatórias ausentes"));
        assertTrue(exception.getMessage().contains("aws.region não configurado"));
        assertTrue(exception.getMessage().contains("aws.s3.bucket-name não configurado"));
        assertTrue(exception.getMessage().contains("aws.sqs.queue-url não configurado"));
    }

    @Test
    void testValidateConfigurationSucceedsWithAllConfigurationsPresent() {
        // Arrange
        AwsConfigurationValidator validator = new AwsConfigurationValidator();
        ReflectionTestUtils.setField(validator, "region", "us-east-1");
        ReflectionTestUtils.setField(validator, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(validator, "queueUrl", "https://sqs.us-east-1.amazonaws.com/123456789/test-queue");

        // Act & Assert
        assertDoesNotThrow(validator::validateConfiguration);
    }

    @Test
    void testValidateConfigurationSucceedsWithMinimalValidValues() {
        // Arrange
        AwsConfigurationValidator validator = new AwsConfigurationValidator();
        ReflectionTestUtils.setField(validator, "region", "a");
        ReflectionTestUtils.setField(validator, "bucketName", "b");
        ReflectionTestUtils.setField(validator, "queueUrl", "c");

        // Act & Assert
        assertDoesNotThrow(validator::validateConfiguration);
    }
}
