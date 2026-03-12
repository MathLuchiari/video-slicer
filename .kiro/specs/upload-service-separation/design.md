# Design Document: Upload Service Separation

## Overview

This design document specifies the technical approach for refactoring the video-slicer repository into a dedicated video upload service. The refactoring involves removing all video slicing functionality while preserving the upload capabilities and maintaining the existing hexagonal architecture.

The current system combines two distinct responsibilities:
1. Video upload with S3 storage and SQS event publishing
2. Video slicing/frame extraction using JavaCV

This refactoring separates these concerns by creating a focused upload service that handles only video ingestion, validation, storage, and event publishing. The slicing functionality will be removed entirely from this codebase.

### Key Design Goals

- Maintain single responsibility principle (upload only)
- Preserve hexagonal architecture and clean separation of concerns
- Keep all existing upload functionality intact
- Remove all slicing-related code and dependencies
- Update naming to reflect the service's focused purpose
- Ensure backward compatibility for upload API consumers

### Refactoring Strategy

The refactoring follows a systematic approach:

1. **Removal Phase**: Delete slicing-related classes, methods, and dependencies
2. **Renaming Phase**: Update package names, artifact IDs, and class names
3. **Cleanup Phase**: Remove unused domain classes and imports
4. **Documentation Phase**: Update README and configuration files
5. **Verification Phase**: Ensure all tests pass and upload functionality works

## Architecture

### Current Architecture

The system follows hexagonal architecture (ports and adapters pattern) with three main layers:

```
com.videoslicer.video_slicer/
├── domain/                    # Business entities and rules
│   ├── video/
│   │   ├── VideoMetadata     # Core video information
│   │   ├── VideoValidation   # Validation rules
│   │   ├── Video             # Video entity (unused)
│   └── user/                 # User entities (unused)
│       ├── User
│       ├── Email
│       └── Password
├── application/              # Use cases and ports
│   ├── usecases/
│   │   ├── UploadVideoUseCase
│   │   ├── SliceVideoUseCase  # TO BE REMOVED
│   │   └── impl/
│   │       ├── UploadVideoUseCaseImpl
│   │       └── SliceVideoUseCaseImpl  # TO BE REMOVED
│   └── ports/
│       ├── S3StorageService
│       └── SQSPublisherService
├── adapters/                 # External integrations
│   ├── controllers/
│   │   └── VideoController
│   └── aws/
│       ├── AwsS3StorageAdapter
│       └── AwsSqsPublisherAdapter
└── config/                   # Configuration
    ├── AwsConfiguration
    └── AwsConfigurationValidator
```

### Target Architecture

After refactoring, the structure will be:

```
com.videoupload.video_upload_service/
├── domain/
│   └── video/
│       ├── VideoMetadata
│       └── VideoValidation
├── application/
│   ├── usecases/
│   │   ├── UploadVideoUseCase
│   │   └── impl/
│   │       └── UploadVideoUseCaseImpl
│   └── ports/
│       ├── S3StorageService
│       └── SQSPublisherService
├── adapters/
│   ├── controllers/
│   │   └── VideoController
│   └── aws/
│       ├── AwsS3StorageAdapter
│       └── AwsSqsPublisherAdapter
└── config/
    ├── AwsConfiguration
    └── AwsConfigurationValidator
```

### Architectural Principles Preserved

1. **Dependency Inversion**: Application layer depends on port interfaces, not concrete implementations
2. **Separation of Concerns**: Domain logic isolated from infrastructure concerns
3. **Testability**: Ports allow easy mocking and testing without AWS dependencies
4. **Single Responsibility**: Each class has one clear purpose
5. **Open/Closed**: New storage or messaging implementations can be added without modifying use cases

## Components and Interfaces

### Domain Layer

#### VideoMetadata

Core domain entity representing video information:

```java
package com.videoupload.video_upload_service.domain.video;

public class VideoMetadata {
    private final UUID videoId;
    private final String fileName;
    private final String extension;
    private final long sizeInBytes;
    private final Instant uploadTimestamp;
    
    // Constructor, getters, equals, hashCode
}
```

**Responsibilities**:
- Encapsulate video metadata
- Provide immutable value object
- Support serialization to JSON for SQS messages

**Changes**: Package rename only

#### VideoValidation

Domain service for video validation rules:

```java
package com.videoupload.video_upload_service.domain.video;

public class VideoValidation {
    private static final Set<String> SUPPORTED_EXTENSIONS = 
        Set.of("mp4", "avi", "mov", "mkv", "webm");
    
    public static ValidationResult validate(MultipartFile file, long maxSizeBytes);
    
    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;
    }
}
```

**Responsibilities**:
- Validate file is not empty
- Validate file extension is supported
- Validate file size within limits
- Return descriptive error messages

**Changes**: Package rename only

### Application Layer

#### UploadVideoUseCase (Port)

```java
package com.videoupload.video_upload_service.application.usecases;

public interface UploadVideoUseCase {
    UploadResult execute(MultipartFile file);
    
    class UploadResult {
        public final boolean success;
        public final String message;
        public final UUID videoId;
        public final String fileName;
        public final String videoUrl;
    }
}
```

**Responsibilities**:
- Define upload operation contract
- Encapsulate upload result data

**Changes**: Package rename only

#### UploadVideoUseCaseImpl

```java
package com.videoupload.video_upload_service.application.usecases.impl;

public class UploadVideoUseCaseImpl implements UploadVideoUseCase {
    private final S3StorageService storageService;
    private final SQSPublisherService publisherService;
    
    @Override
    public UploadResult execute(MultipartFile file) {
        // 1. Validate video
        // 2. Create VideoMetadata
        // 3. Store in S3
        // 4. Publish to SQS (non-blocking failure)
        // 5. Return result
    }
}
```

**Responsibilities**:
- Orchestrate upload workflow
- Coordinate between validation, storage, and messaging
- Handle errors gracefully
- Ensure SQS failure doesn't fail upload

**Changes**: Package rename only

#### S3StorageService (Port)

```java
package com.videoupload.video_upload_service.application.ports;

public interface S3StorageService {
    String store(VideoMetadata videoMetadata, InputStream inputStream) 
        throws StorageException;
    
    class StorageException extends Exception {
        public StorageException(String message, Throwable cause);
    }
}
```

**Responsibilities**:
- Define storage contract
- Abstract S3 implementation details

**Changes**: 
- Remove `retrieve()` method (only used for slicing)
- Package rename

#### SQSPublisherService (Port)

```java
package com.videoupload.video_upload_service.application.ports;

public interface SQSPublisherService {
    void publish(VideoMetadata metadata) throws PublishException;
    
    class PublishException extends Exception {
        public PublishException(String message, Throwable cause);
    }
}
```

**Responsibilities**:
- Define message publishing contract
- Abstract SQS implementation details

**Changes**: Package rename only

### Adapters Layer

#### VideoController

```java
package com.videoupload.video_upload_service.adapters.controllers;

@RestController
@RequestMapping("/api/video")
public class VideoController {
    private final UploadVideoUseCase uploadUseCase;
    
    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> upload(@RequestParam MultipartFile file) {
        // Execute upload use case
        // Return 201 with videoId, fileName, videoUrl on success
        // Return 400 for validation errors
        // Return 500 for storage errors
    }
}
```

**Responsibilities**:
- Expose REST API for video upload
- Map HTTP requests to use case calls
- Map use case results to HTTP responses
- Handle error status codes

**Changes**:
- Remove `SliceVideoUseCase` dependency
- Remove `/slice` endpoint
- Package rename

#### AwsS3StorageAdapter

```java
package com.videoupload.video_upload_service.adapters.aws;

@Service
public class AwsS3StorageAdapter implements S3StorageService {
    private final S3Client s3Client;
    private final String bucketName;
    private final String folderPrefix;
    
    @Override
    public String store(VideoMetadata metadata, InputStream inputStream) 
        throws StorageException {
        // Build S3 key: {folderPrefix}{videoId}.{extension}
        // Set content type based on extension
        // Upload with retry logic (3 attempts, exponential backoff)
        // Return S3 URL
    }
}
```

**Responsibilities**:
- Implement S3 storage operations
- Handle AWS SDK interactions
- Implement retry mechanism with exponential backoff
- Generate S3 URLs

**Changes**:
- Remove `retrieve()` method implementation
- Package rename

#### AwsSqsPublisherAdapter

```java
package com.videoupload.video_upload_service.adapters.aws;

@Service
public class AwsSqsPublisherAdapter implements SQSPublisherService {
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;
    
    @Override
    public void publish(VideoMetadata metadata) throws PublishException {
        // Serialize metadata to JSON
        // Send message to SQS
        // Retry up to 3 times with exponential backoff
    }
}
```

**Responsibilities**:
- Implement SQS message publishing
- Serialize VideoMetadata to JSON
- Handle AWS SDK interactions
- Implement retry mechanism

**Changes**: Package rename only

### Configuration Layer

#### AwsConfiguration

```java
package com.videoupload.video_upload_service.config;

@Configuration
public class AwsConfiguration {
    @Bean
    public S3Client s3Client(@Value("${aws.region}") String region,
                             @Value("${aws.accessKeyId}") String accessKey,
                             @Value("${aws.secretAccessKey}") String secretKey);
    
    @Bean
    public SqsClient sqsClient(@Value("${aws.region}") String region,
                               @Value("${aws.accessKeyId}") String accessKey,
                               @Value("${aws.secretAccessKey}") String secretKey);
    
    @Bean
    public ObjectMapper objectMapper();
}
```

**Responsibilities**:
- Configure AWS SDK clients
- Provide dependency injection beans
- Read configuration from application.properties

**Changes**: Package rename only

#### AwsConfigurationValidator

```java
package com.videoupload.video_upload_service.config;

@Component
public class AwsConfigurationValidator implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        // Validate AWS region is set
        // Validate AWS credentials are set
        // Validate S3 bucket name is set
        // Validate SQS queue URL is set
        // Throw exception if any required config is missing
    }
}
```

**Responsibilities**:
- Validate required AWS configuration at startup
- Fail fast if configuration is incomplete
- Provide clear error messages

**Changes**: Package rename only

## Data Models

### VideoMetadata

```java
public class VideoMetadata {
    private final UUID videoId;
    private final String fileName;
    private final String extension;
    private final long sizeInBytes;
    private final Instant uploadTimestamp;
}
```

**Fields**:
- `videoId`: Unique identifier (UUID v4)
- `fileName`: Original filename from upload
- `extension`: File extension (mp4, avi, mov, mkv, webm)
- `sizeInBytes`: File size in bytes
- `uploadTimestamp`: ISO-8601 timestamp of upload

**Serialization**: Jackson JSON for SQS messages

**Validation**: Performed by VideoValidation before creation

### API Request/Response Models

#### Upload Request

```
POST /api/video/upload
Content-Type: multipart/form-data

file: <video file>
```

#### Upload Success Response (201 Created)

```json
{
  "message": "Vídeo enviado com sucesso",
  "content": {
    "videoId": "550e8400-e29b-41d4-a716-446655440000",
    "fileName": "example.mp4",
    "videoUrl": "https://fiap-hackatoon-videos.s3.us-east-1.amazonaws.com/videos/550e8400-e29b-41d4-a716-446655440000.mp4"
  }
}
```

#### Upload Validation Error Response (400 Bad Request)

```json
{
  "message": "Extensão de arquivo não suportada. Use: mp4, avi, mov, mkv, webm"
}
```

#### Upload Storage Error Response (500 Internal Server Error)

```json
{
  "message": "Erro ao armazenar vídeo no S3"
}
```

### SQS Message Format

```json
{
  "videoId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "example.mp4",
  "extension": "mp4",
  "sizeInBytes": 10485760,
  "uploadTimestamp": "2024-01-15T10:30:00Z"
}
```

### Configuration Properties

```properties
# Application
spring.application.name=video-upload-service
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB

# AWS
aws.region=us-east-1
aws.accessKeyId=<access-key>
aws.secretAccessKey=<secret-key>

# S3
aws.s3.bucket-name=<bucket-name>
aws.s3.folder-prefix=videos/

# SQS
aws.sqs.queue-url=<queue-url>
```

### Maven Artifact Configuration

```xml
<groupId>com.videoupload</groupId>
<artifactId>video-upload-service</artifactId>
<version>0.0.1-SNAPSHOT</version>
<name>video-upload-service</name>
<description>Video upload service with S3 storage and SQS event publishing</description>
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: File Format Validation

*For any* uploaded file, the system should accept files with extensions mp4, avi, mov, mkv, or webm, and reject all other file extensions with a descriptive error message.

**Validates: Requirements 3.1, 4.2**

### Property 2: File Size Validation

*For any* uploaded file, the system should reject files exceeding the configured maximum size (default 500MB) and return HTTP 400 with a descriptive error message.

**Validates: Requirements 3.2, 4.3, 12.3**

### Property 3: Successful Upload Workflow

*For any* valid video file (correct format and size), when uploaded successfully, the system should:
- Store the file in S3 with the configured bucket and folder prefix
- Publish metadata to the SQS queue
- Return HTTP 201 with videoId, fileName, and a valid S3 URL

**Validates: Requirements 3.3, 3.4, 3.5, 5.7**

### Property 4: Validation Error Response

*For any* file that fails validation (empty, unsupported format, or exceeds size limit), the system should return HTTP 400 with a non-empty descriptive error message explaining the specific validation failure.

**Validates: Requirements 3.6, 4.4, 11.2**

### Property 5: Storage Error Response

*For any* upload where S3 storage fails after all retry attempts, the system should return HTTP 500 with a descriptive error message.

**Validates: Requirements 3.7, 11.3**

### Property 6: Content-Type Mapping

*For any* video file stored in S3, the Content-Type header should be set according to the file extension:
- mp4 → video/mp4
- avi → video/x-msvideo
- mov → video/quicktime
- mkv → video/x-matroska
- webm → video/webm

**Validates: Requirements 5.3**

### Property 7: S3 Key Uniqueness and Format

*For any* uploaded video, the generated S3 key should:
- Follow the format: `{folderPrefix}{videoId}.{extension}`
- Use a unique UUID v4 for videoId
- Include the original file extension
- Be unique across all uploads (no collisions)

**Validates: Requirements 5.4**

### Property 8: SQS Message Serialization Round-Trip

*For any* VideoMetadata object, serializing to JSON and then deserializing should produce an equivalent object with all fields preserved (videoId, fileName, extension, sizeInBytes, uploadTimestamp).

**Validates: Requirements 6.2, 6.3**

### Property 9: SQS Failure Resilience

*For any* upload where SQS publishing fails (even after retries), the upload operation should still succeed and return HTTP 201 with the video details, while logging the SQS error.

**Validates: Requirements 6.5, 11.4**

### Property 10: Error Logging

*For any* exception that occurs during the upload process (validation, storage, or messaging), the system should log the error with appropriate context before returning the error response.

**Validates: Requirements 11.1**

## Error Handling

### Error Categories

The upload service handles three main categories of errors:

1. **Validation Errors** (HTTP 400)
   - Empty file
   - Unsupported file extension
   - File size exceeds limit
   - Missing required parameters

2. **Storage Errors** (HTTP 500)
   - S3 connection failures
   - S3 authentication errors
   - S3 bucket access denied
   - Network timeouts

3. **Messaging Errors** (Non-blocking)
   - SQS connection failures
   - SQS authentication errors
   - Message serialization errors
   - Queue not found

### Retry Strategy

Both S3 and SQS operations implement exponential backoff retry:

```
Attempt 1: Immediate
Attempt 2: Wait 1 second
Attempt 3: Wait 2 seconds
Maximum attempts: 3
```

**Implementation**:
```java
private <T> T executeWithRetry(Supplier<T> operation, String operationName) {
    int maxAttempts = 3;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            return operation.get();
        } catch (Exception e) {
            if (attempt == maxAttempts) {
                throw new StorageException("Failed after " + maxAttempts + " attempts", e);
            }
            long waitTime = (long) Math.pow(2, attempt - 1) * 1000;
            Thread.sleep(waitTime);
        }
    }
}
```

### Error Response Format

All error responses follow a consistent format:

```json
{
  "message": "Descriptive error message"
}
```

### Logging Strategy

- **ERROR level**: Storage failures, unexpected exceptions
- **WARN level**: SQS publishing failures (non-blocking)
- **INFO level**: Successful uploads, retry attempts
- **DEBUG level**: Detailed operation traces

### Graceful Degradation

The service implements graceful degradation for non-critical failures:

- **SQS Publishing Failure**: Upload succeeds, error logged, downstream processing delayed
- **Partial S3 Metadata**: Upload succeeds with available metadata
- **Configuration Warnings**: Service starts with defaults where appropriate

### Validation Error Messages

Specific, actionable error messages for each validation failure:

- Empty file: "O arquivo está vazio"
- Unsupported format: "Extensão de arquivo não suportada. Use: mp4, avi, mov, mkv, webm"
- Size exceeded: "Arquivo excede o tamanho máximo permitido de {maxSize}MB"
- Missing file: "Nenhum arquivo foi enviado"

## Testing Strategy

### Dual Testing Approach

The upload service requires both unit testing and property-based testing for comprehensive coverage:

- **Unit tests**: Verify specific examples, edge cases, and integration points
- **Property tests**: Verify universal properties across randomized inputs

Both approaches are complementary and necessary. Unit tests catch concrete bugs and verify specific scenarios, while property tests verify general correctness across a wide input space.

### Property-Based Testing

**Framework**: jqwik (Java property-based testing library)

**Configuration**:
- Minimum 100 iterations per property test
- Each test tagged with feature name and property reference
- Tag format: `@Tag("Feature: upload-service-separation, Property {number}: {property_text}")`

**Property Test Examples**:

```java
@Property
@Tag("Feature: upload-service-separation, Property 1: File Format Validation")
void fileFormatValidation(@ForAll("videoFiles") MockMultipartFile file) {
    // Generate files with various extensions
    // Verify only supported formats pass validation
    // Verify unsupported formats return 400 with descriptive message
}

@Property
@Tag("Feature: upload-service-separation, Property 8: SQS Message Serialization Round-Trip")
void sqsMessageSerializationRoundTrip(@ForAll("videoMetadata") VideoMetadata metadata) {
    // Serialize metadata to JSON
    // Deserialize back to object
    // Verify all fields match original
}
```

**Generators Required**:
- Video files with random extensions (supported and unsupported)
- Video files with random sizes (within and exceeding limits)
- VideoMetadata with random but valid values
- Empty files and edge cases

### Unit Testing

**Focus Areas**:
1. **Controller Layer**
   - Endpoint mapping verification
   - Request parameter binding
   - Response status codes
   - Error response format

2. **Use Case Layer**
   - Workflow orchestration
   - Error handling paths
   - SQS failure resilience
   - Validation integration

3. **Adapter Layer**
   - AWS SDK interaction mocking
   - Retry mechanism verification
   - Configuration injection
   - Exception translation

4. **Domain Layer**
   - VideoValidation logic
   - VideoMetadata creation
   - Edge cases (empty files, boundary sizes)

**Example Unit Tests**:

```java
@Test
void uploadEmptyFile_shouldReturnBadRequest() {
    // Arrange: Create empty file
    // Act: Call upload endpoint
    // Assert: Returns 400 with "arquivo está vazio"
}

@Test
void uploadWithSQSFailure_shouldStillSucceed() {
    // Arrange: Mock SQS to throw exception
    // Act: Upload valid file
    // Assert: Returns 201, logs SQS error
}

@Test
void s3StorageFailureAfterRetries_shouldReturn500() {
    // Arrange: Mock S3 to fail 3 times
    // Act: Upload valid file
    // Assert: Returns 500, attempted 3 retries
}
```

### Integration Testing

**Scope**: End-to-end upload workflow with real AWS services (or LocalStack)

**Test Cases**:
1. Complete upload workflow (upload → S3 → SQS → response)
2. Large file upload (near 500MB limit)
3. Concurrent uploads
4. AWS credential validation at startup
5. Configuration loading from application.properties

**Tools**:
- Spring Boot Test
- TestContainers (for LocalStack)
- MockMvc for HTTP testing

### Test Coverage Goals

- **Line Coverage**: Minimum 80%
- **Branch Coverage**: Minimum 75%
- **Property Tests**: All 10 correctness properties implemented
- **Unit Tests**: All edge cases and error paths covered
- **Integration Tests**: Happy path and critical failure scenarios

### Backward Compatibility Testing

Verify that existing upload API consumers are not affected:

```java
@Test
void uploadEndpoint_maintainsBackwardCompatibility() {
    // Verify POST /api/video/upload still works
    // Verify response format unchanged
    // Verify videoUrl format unchanged
}
```

### Refactoring Verification Tests

Tests to verify the refactoring was successful:

```java
@Test
void sliceEndpoint_shouldNotExist() {
    // Attempt GET /api/video/slice
    // Assert: 404 Not Found
}

@Test
void sliceVideoUseCase_shouldNotExist() {
    // Use reflection to verify class doesn't exist
    // Assert: ClassNotFoundException
}

@Test
void javacvDependency_shouldNotExist() {
    // Parse pom.xml
    // Assert: No javacv-platform dependency
}
```

### Test Execution Strategy

1. **Pre-commit**: Fast unit tests and property tests (< 30 seconds)
2. **CI Pipeline**: All tests including integration tests
3. **Pre-deployment**: Full test suite + manual smoke tests
4. **Post-deployment**: Health check and upload verification

