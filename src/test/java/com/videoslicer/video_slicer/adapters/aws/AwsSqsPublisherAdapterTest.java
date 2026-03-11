package com.videoslicer.video_slicer.adapters.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoslicer.video_slicer.application.ports.SQSPublisherService;
import com.videoslicer.video_slicer.domain.video.VideoMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para AwsSqsPublisherAdapter
 * 
 * Validates: Requirements 3.1, 3.5, 6.2
 */
@ExtendWith(MockitoExtension.class)
class AwsSqsPublisherAdapterTest {

    @Mock
    private SqsClient sqsClient;

    private AwsSqsPublisherAdapter adapter;
    private ObjectMapper objectMapper;
    
    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/test-queue";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new AwsSqsPublisherAdapter(sqsClient, QUEUE_URL, objectMapper);
    }

    @Test
    void testPublishSuccessDoesNotThrowException() throws Exception {
        // Arrange
        UUID videoId = UUID.randomUUID();
        VideoMetadata metadata = new VideoMetadata(
            videoId, "test-video.mp4", "mp4", 52428800L, Instant.now()
        );
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().build());

        // Act & Assert
        assertDoesNotThrow(() -> adapter.publish(metadata));
        verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void testPublishFailsAfter3RetriesThrowsPublishException() {
        // Arrange
        UUID videoId = UUID.randomUUID();
        VideoMetadata metadata = new VideoMetadata(
            videoId, "test-video.mp4", "mp4", 52428800L, Instant.now()
        );
        
        SqsException sqsException = (SqsException) SqsException.builder()
            .message("Network timeout")
            .build();
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenThrow(sqsException);

        // Act & Assert
        SQSPublisherService.PublishException exception = assertThrows(
            SQSPublisherService.PublishException.class,
            () -> adapter.publish(metadata)
        );
        
        assertTrue(exception.getMessage().contains("3 tentativas"));
        verify(sqsClient, times(3)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void testPublishSucceedsOnSecondAttemptAfterFirstFailure() throws Exception {
        // Arrange
        UUID videoId = UUID.randomUUID();
        VideoMetadata metadata = new VideoMetadata(
            videoId, "test-video.mp4", "mp4", 52428800L, Instant.now()
        );
        
        SqsException sqsException = (SqsException) SqsException.builder()
            .message("Temporary failure")
            .build();
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenThrow(sqsException)
            .thenReturn(SendMessageResponse.builder().build());

        // Act & Assert
        assertDoesNotThrow(() -> adapter.publish(metadata));
        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void testMessageIsValidJsonWithCorrectFields() throws Exception {
        // Arrange
        UUID videoId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        VideoMetadata metadata = new VideoMetadata(
            videoId, "test-video.mp4", "mp4", 52428800L, timestamp
        );
        
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().build());

        // Act
        adapter.publish(metadata);

        // Assert
        verify(sqsClient).sendMessage(requestCaptor.capture());
        SendMessageRequest capturedRequest = requestCaptor.getValue();
        
        // Verify queue URL
        assertEquals(QUEUE_URL, capturedRequest.queueUrl());
        
        // Parse and verify JSON message body
        String messageBody = capturedRequest.messageBody();
        assertNotNull(messageBody);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> messageMap = objectMapper.readValue(messageBody, Map.class);
        
        // Verify all required fields are present
        assertTrue(messageMap.containsKey("videoId"));
        assertTrue(messageMap.containsKey("fileName"));
        assertTrue(messageMap.containsKey("extension"));
        assertTrue(messageMap.containsKey("sizeInBytes"));
        assertTrue(messageMap.containsKey("uploadTimestamp"));
        
        // Verify field values
        assertEquals(videoId.toString(), messageMap.get("videoId"));
        assertEquals("test-video.mp4", messageMap.get("fileName"));
        assertEquals("mp4", messageMap.get("extension"));
        assertEquals(52428800, ((Number) messageMap.get("sizeInBytes")).longValue());
        assertEquals(timestamp.toString(), messageMap.get("uploadTimestamp"));
    }

    @Test
    void testMessageContainsAllMetadataFields() throws Exception {
        // Arrange
        UUID videoId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2024-01-15T10:30:00Z");
        VideoMetadata metadata = new VideoMetadata(
            videoId, "my-video.avi", "avi", 104857600L, timestamp
        );
        
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().build());

        // Act
        adapter.publish(metadata);

        // Assert
        verify(sqsClient).sendMessage(requestCaptor.capture());
        String messageBody = requestCaptor.getValue().messageBody();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> messageMap = objectMapper.readValue(messageBody, Map.class);
        
        assertEquals(videoId.toString(), messageMap.get("videoId"));
        assertEquals("my-video.avi", messageMap.get("fileName"));
        assertEquals("avi", messageMap.get("extension"));
        assertEquals(104857600, ((Number) messageMap.get("sizeInBytes")).longValue());
        assertEquals(timestamp.toString(), messageMap.get("uploadTimestamp"));
    }

    @Test
    void testPublishUsesCorrectQueueUrl() throws Exception {
        // Arrange
        UUID videoId = UUID.randomUUID();
        VideoMetadata metadata = new VideoMetadata(
            videoId, "test.mp4", "mp4", 1024L, Instant.now()
        );
        
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().build());

        // Act
        adapter.publish(metadata);

        // Assert
        verify(sqsClient).sendMessage(requestCaptor.capture());
        assertEquals(QUEUE_URL, requestCaptor.getValue().queueUrl());
    }

    @Test
    void testPublishWithDifferentVideoExtensions() throws Exception {
        // Test with different video extensions
        String[] extensions = {"mp4", "avi", "mov", "mkv", "webm"};
        
        for (String extension : extensions) {
            // Arrange
            UUID videoId = UUID.randomUUID();
            VideoMetadata metadata = new VideoMetadata(
                videoId, "video." + extension, extension, 1024L, Instant.now()
            );
            
            ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
            
            when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().build());

            // Act
            adapter.publish(metadata);

            // Assert
            verify(sqsClient, atLeastOnce()).sendMessage(requestCaptor.capture());
            String messageBody = requestCaptor.getValue().messageBody();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> messageMap = objectMapper.readValue(messageBody, Map.class);
            assertEquals(extension, messageMap.get("extension"));
        }
    }

    @Test
    void testPublishWithLargeFileSize() throws Exception {
        // Arrange - Test with 500MB file
        UUID videoId = UUID.randomUUID();
        long largeFileSize = 500L * 1024 * 1024; // 500MB
        VideoMetadata metadata = new VideoMetadata(
            videoId, "large-video.mp4", "mp4", largeFileSize, Instant.now()
        );
        
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().build());

        // Act
        adapter.publish(metadata);

        // Assert
        verify(sqsClient).sendMessage(requestCaptor.capture());
        String messageBody = requestCaptor.getValue().messageBody();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> messageMap = objectMapper.readValue(messageBody, Map.class);
        assertEquals(largeFileSize, ((Number) messageMap.get("sizeInBytes")).longValue());
    }

    @Test
    void testPublishInterruptedDuringRetryThrowsPublishException() {
        // Arrange
        UUID videoId = UUID.randomUUID();
        VideoMetadata metadata = new VideoMetadata(
            videoId, "test.mp4", "mp4", 1024L, Instant.now()
        );
        
        SqsException sqsException = (SqsException) SqsException.builder()
            .message("Network error")
            .build();
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenThrow(sqsException);
        
        // Interrupt the thread before the test
        Thread.currentThread().interrupt();

        // Act & Assert
        SQSPublisherService.PublishException exception = assertThrows(
            SQSPublisherService.PublishException.class,
            () -> adapter.publish(metadata)
        );
        
        assertTrue(exception.getMessage().contains("interrompida"));
        
        // Clear the interrupted status
        Thread.interrupted();
    }

    // ========== Property-Based Tests ==========

    /**
     * Property 3: SQS Message Structure
     * 
     * Validates: Requirements 3.2, 3.3, 3.5
     * 
     * For any video successfully uploaded to S3, the message published to SQS SHALL be valid JSON 
     * containing fields: videoId (UUID string), fileName (string), extension (string), 
     * sizeInBytes (positive long), and uploadTimestamp (ISO-8601 string).
     */
    @net.jqwik.api.Property(tries = 100)
    @net.jqwik.api.Label("Feature: aws-s3-sqs-integration, Property 3: SQS Message Structure")
    void sqsMessageStructureProperty(
        @net.jqwik.api.ForAll("validVideoMetadata") VideoMetadata metadata
    ) throws Exception {
        // Arrange - Create fresh mocks for each property test iteration
        SqsClient mockSqsClient = mock(SqsClient.class);
        ObjectMapper testObjectMapper = new ObjectMapper();
        AwsSqsPublisherAdapter testAdapter = new AwsSqsPublisherAdapter(
            mockSqsClient, QUEUE_URL, testObjectMapper
        );
        
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        
        when(mockSqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().build());

        // Act
        testAdapter.publish(metadata);

        // Assert
        verify(mockSqsClient, times(1)).sendMessage(requestCaptor.capture());
        String messageBody = requestCaptor.getValue().messageBody();
        
        // Verify message is valid JSON
        assertNotNull(messageBody, "Message body should not be null");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> messageMap = testObjectMapper.readValue(messageBody, Map.class);
        
        // Verify all required fields are present
        assertTrue(messageMap.containsKey("videoId"), "Message must contain videoId field");
        assertTrue(messageMap.containsKey("fileName"), "Message must contain fileName field");
        assertTrue(messageMap.containsKey("extension"), "Message must contain extension field");
        assertTrue(messageMap.containsKey("sizeInBytes"), "Message must contain sizeInBytes field");
        assertTrue(messageMap.containsKey("uploadTimestamp"), "Message must contain uploadTimestamp field");
        
        // Verify videoId is a valid UUID string
        String videoIdStr = (String) messageMap.get("videoId");
        assertNotNull(videoIdStr, "videoId should not be null");
        assertDoesNotThrow(() -> UUID.fromString(videoIdStr), 
            "videoId should be a valid UUID string");
        assertEquals(metadata.getVideoId().toString(), videoIdStr, 
            "videoId should match the metadata");
        
        // Verify fileName is a non-empty string
        String fileName = (String) messageMap.get("fileName");
        assertNotNull(fileName, "fileName should not be null");
        assertFalse(fileName.isEmpty(), "fileName should not be empty");
        assertEquals(metadata.getFileName(), fileName, 
            "fileName should match the metadata");
        
        // Verify extension is a non-empty string
        String extension = (String) messageMap.get("extension");
        assertNotNull(extension, "extension should not be null");
        assertFalse(extension.isEmpty(), "extension should not be empty");
        assertEquals(metadata.getExtension(), extension, 
            "extension should match the metadata");
        
        // Verify sizeInBytes is a positive long
        Object sizeObj = messageMap.get("sizeInBytes");
        assertNotNull(sizeObj, "sizeInBytes should not be null");
        long sizeInBytes = ((Number) sizeObj).longValue();
        assertTrue(sizeInBytes > 0, "sizeInBytes should be positive");
        assertEquals(metadata.getSizeInBytes(), sizeInBytes, 
            "sizeInBytes should match the metadata");
        
        // Verify uploadTimestamp is a valid ISO-8601 string
        String timestampStr = (String) messageMap.get("uploadTimestamp");
        assertNotNull(timestampStr, "uploadTimestamp should not be null");
        assertDoesNotThrow(() -> Instant.parse(timestampStr), 
            "uploadTimestamp should be a valid ISO-8601 string");
        assertEquals(metadata.getUploadTimestamp().toString(), timestampStr, 
            "uploadTimestamp should match the metadata");
    }

    /**
     * Property 7: SQS Retry Mechanism
     * 
     * Validates: Requirements 6.2
     * 
     * For any SQS operation that fails with a retryable error (timeout, network error), 
     * exactly 3 attempts SHALL be made with exponential backoff before throwing PublishException.
     */
    @net.jqwik.api.Property(tries = 100)
    @net.jqwik.api.Label("Feature: aws-s3-sqs-integration, Property 7: SQS Retry Mechanism")
    void sqsRetryMechanismProperty(
        @net.jqwik.api.ForAll("validVideoMetadata") VideoMetadata metadata
    ) {
        // Arrange - Create fresh mocks for each property test iteration
        SqsClient mockSqsClient = mock(SqsClient.class);
        ObjectMapper testObjectMapper = new ObjectMapper();
        AwsSqsPublisherAdapter testAdapter = new AwsSqsPublisherAdapter(
            mockSqsClient, QUEUE_URL, testObjectMapper
        );
        
        // Simulate retryable failure (network error)
        SqsException sqsException = (SqsException) SqsException.builder()
            .message("Network timeout - retryable error")
            .build();
        
        when(mockSqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenThrow(sqsException);

        // Act & Assert - Verify that PublishException is thrown after retries
        long startTime = System.currentTimeMillis();
        
        SQSPublisherService.PublishException exception = assertThrows(
            SQSPublisherService.PublishException.class,
            () -> testAdapter.publish(metadata),
            "Should throw PublishException after all retries fail"
        );
        
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        
        // Verify exactly 3 attempts were made
        verify(mockSqsClient, times(3)).sendMessage(any(SendMessageRequest.class));
        
        // Verify exception message mentions the number of retries
        assertTrue(exception.getMessage().contains("3 tentativas"),
            "Exception message should mention 3 attempts");
        
        // Verify exponential backoff timing (minimum only to avoid flakiness)
        // Expected backoff: 2s (after attempt 1) + 4s (after attempt 2) = 6000ms minimum
        // Note: We only check minimum to avoid flakiness from test execution overhead
        long expectedMinDuration = 5500; // 5.5 seconds with some tolerance
        
        assertTrue(totalDuration >= expectedMinDuration,
            String.format("Total duration should be at least %dms (exponential backoff: 2s + 4s), but was %dms",
                expectedMinDuration, totalDuration));
    }

    /**
     * Generator for valid VideoMetadata instances
     */
    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<VideoMetadata> validVideoMetadata() {
        // Generate random UUIDs
        net.jqwik.api.Arbitrary<UUID> uuids = net.jqwik.api.Arbitraries.randomValue(
            random -> UUID.randomUUID()
        );
        
        // Generate random file names (alphanumeric with dashes and underscores)
        net.jqwik.api.Arbitrary<String> fileNames = net.jqwik.api.Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(5)
            .ofMaxLength(50)
            .map(s -> s + "-video");
        
        // Generate valid video extensions
        net.jqwik.api.Arbitrary<String> extensions = net.jqwik.api.Arbitraries.of(
            "mp4", "avi", "mov", "mkv", "webm"
        );
        
        // Generate file sizes between 1 byte and 500MB
        net.jqwik.api.Arbitrary<Long> fileSizes = net.jqwik.api.Arbitraries.longs()
            .between(1L, 500L * 1024 * 1024);
        
        // Generate timestamps within the last year
        net.jqwik.api.Arbitrary<Instant> timestamps = net.jqwik.api.Arbitraries.longs()
            .between(
                Instant.now().minusSeconds(365L * 24 * 60 * 60).toEpochMilli(),
                Instant.now().toEpochMilli()
            )
            .map(Instant::ofEpochMilli);
        
        // Combine all arbitraries to create VideoMetadata
        return net.jqwik.api.Combinators.combine(
            uuids, fileNames, extensions, fileSizes, timestamps
        ).as(VideoMetadata::new);
    }
}
