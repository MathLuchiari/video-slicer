# Implementation Plan: Upload Service Separation

## Overview

This plan guides the refactoring of the video-slicer repository into a dedicated video upload service. The implementation follows a systematic approach: first removing slicing functionality, then renaming packages and artifacts, cleaning up unused code, updating documentation, and finally implementing property-based tests to verify correctness.

The refactoring maintains the existing hexagonal architecture while focusing the service on a single responsibility: video upload with S3 storage and SQS event publishing.

## Tasks

- [ ] 1. Remove slicing functionality and dependencies
  - [ ] 1.1 Delete SliceVideoUseCase interface and SliceVideoUseCaseImpl implementation
    - Remove `com/videoslicer/video_slicer/application/usecases/SliceVideoUseCase.java`
    - Remove `com/videoslicer/video_slicer/application/usecases/impl/SliceVideoUseCaseImpl.java`
    - _Requirements: 1.1, 1.2_

  - [ ] 1.2 Remove /slice endpoint from VideoController
    - Delete the `/api/video/slice` endpoint method and its SliceVideoUseCase dependency
    - _Requirements: 1.3_

  - [ ] 1.3 Remove javacv-platform dependency from pom.xml
    - Delete the javacv-platform dependency entry
    - _Requirements: 1.4_

  - [ ] 1.4 Remove retrieve method from S3StorageService and AwsS3StorageAdapter
    - Delete the `retrieve()` method from the port interface and adapter implementation
    - _Requirements: 8.3_

- [ ] 2. Rename packages from com.videoslicer to com.videoupload
  - [ ] 2.1 Rename root package structure
    - Rename `com/videoslicer/video_slicer` to `com/videoupload/video_upload_service`
    - Update all package declarations in Java files
    - Update all import statements across the codebase
    - _Requirements: 2.1, 2.5_

  - [ ] 2.2 Rename main application class
    - Rename `VideoSlicerApplication` to `VideoUploadServiceApplication`
    - _Requirements: 2.4_

- [ ] 3. Update Maven artifact configuration
  - [ ] 3.1 Update pom.xml with new artifact details
    - Change `<groupId>` to `com.videoupload`
    - Change `<artifactId>` to `video-upload-service`
    - Change `<name>` to `video-upload-service`
    - Update `<description>` to reflect upload service purpose
    - _Requirements: 2.2_

- [ ] 4. Update application configuration
  - [ ] 4.1 Update application.properties
    - Change `spring.application.name` to `video-upload-service`
    - Verify all AWS configuration properties are present
    - Verify multipart file size limits are configured
    - _Requirements: 2.3, 10.1-10.6, 12.1, 12.2_

- [ ] 5. Remove unused domain classes
  - [ ] 5.1 Analyze and remove unused User domain classes
    - Check if User, Email, and Password classes are referenced
    - If not used, delete `domain/user/User.java`, `domain/user/Email.java`, `domain/user/Password.java`
    - Remove empty user package directory
    - _Requirements: 8.2_

  - [ ] 5.2 Analyze and remove Video domain class if unused
    - Check if Video class is referenced outside of slicing functionality
    - If only used for slicing, delete `domain/video/Video.java`
    - _Requirements: 8.1_

  - [ ] 5.3 Remove .gitkeep files and clean up empty directories
    - Delete any .gitkeep files in package directories
    - Remove empty directories
    - _Requirements: 8.4_

- [ ] 6. Update documentation
  - [ ] 6.1 Rewrite README.md for upload service
    - Update title and description to reflect upload service purpose
    - Document the POST /api/video/upload endpoint with request/response examples
    - Document supported video formats (mp4, avi, mov, mkv, webm)
    - Document required AWS configurations (region, credentials, S3 bucket, SQS queue)
    - Document integration with SQS for downstream processing
    - Remove any references to slicing functionality
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 7. Checkpoint - Verify refactoring completeness
  - Ensure all tests pass, verify no slicing references remain, ask the user if questions arise.

- [ ] 8. Implement property-based tests for validation
  - [ ]* 8.1 Write property test for file format validation
    - **Property 1: File Format Validation**
    - **Validates: Requirements 3.1, 4.2**
    - Generate files with various extensions (supported and unsupported)
    - Verify only mp4, avi, mov, mkv, webm are accepted
    - Verify unsupported formats return HTTP 400 with descriptive message
    - Use jqwik framework with minimum 100 iterations
    - Tag: `@Tag("Feature: upload-service-separation, Property 1: File Format Validation")`

  - [ ]* 8.2 Write property test for file size validation
    - **Property 2: File Size Validation**
    - **Validates: Requirements 3.2, 4.3, 12.3**
    - Generate files with random sizes (within and exceeding 500MB limit)
    - Verify files exceeding limit return HTTP 400 with descriptive message
    - Use jqwik framework with minimum 100 iterations
    - Tag: `@Tag("Feature: upload-service-separation, Property 2: File Size Validation")`

  - [ ]* 8.3 Write property test for successful upload workflow
    - **Property 3: Successful Upload Workflow**
    - **Validates: Requirements 3.3, 3.4, 3.5, 5.7**
    - Generate valid video files (correct format and size)
    - Verify file stored in S3 with correct bucket and folder prefix
    - Verify metadata published to SQS queue
    - Verify HTTP 201 response with videoId, fileName, and valid S3 URL
    - Use jqwik framework with minimum 100 iterations
    - Tag: `@Tag("Feature: upload-service-separation, Property 3: Successful Upload Workflow")`

  - [ ]* 8.4 Write property test for validation error responses
    - **Property 4: Validation Error Response**
    - **Validates: Requirements 3.6, 4.4, 11.2**
    - Generate files that fail validation (empty, unsupported format, exceeds size)
    - Verify HTTP 400 returned with non-empty descriptive error message
    - Verify error message explains specific validation failure
    - Use jqwik framework with minimum 100 iterations
    - Tag: `@Tag("Feature: upload-service-separation, Property 4: Validation Error Response")`

  - [ ]* 8.5 Write property test for storage error responses
    - **Property 5: Storage Error Response**
    - **Validates: Requirements 3.7, 11.3**
    - Mock S3 to fail after all retry attempts
    - Verify HTTP 500 returned with descriptive error message
    - Use jqwik framework with minimum 100 iterations
    - Tag: `@Tag("Feature: upload-service-separation, Property 5: Storage Error Response")`

- [ ] 9. Implement property-based tests for S3 and SQS integration
  - [ ]* 9.1 Write property test for Content-Type mapping
    - **Property 6: Content-Type Mapping**
    - **Validates: Requirements 5.3**
    - Generate files with each supported extension
    - Verify correct Content-Type header set in S3 (mp4→video/mp4, avi→video/x-msvideo, etc.)
    - Use jqwik framework with minimum 100 iterations
    - Tag: `@Tag("Feature: upload-service-separation, Property 6: Content-Type Mapping")`

  - [ ]* 9.2 Write property test for S3 key uniqueness and format
    - **Property 7: S3 Key Uniqueness and Format**
    - **Validates: Requirements 5.4**
    - Generate multiple uploads with random file names and extensions
    - Verify S3 key follows format: `{folderPrefix}{videoId}.{extension}`
    - Verify videoId is a valid UUID v4
    - Verify no key collisions across uploads
    - Use jqwik framework with minimum 100 iterations
    - Tag: `@Tag("Feature: upload-service-separation, Property 7: S3 Key Uniqueness and Format")`

  - [ ]* 9.3 Write property test for SQS message serialization round-trip
    - **Property 8: SQS Message Serialization Round-Trip**
    - **Validates: Requirements 6.2, 6.3**
    - Generate random VideoMetadata objects
    - Serialize to JSON and deserialize back
    - Verify all fields preserved (videoId, fileName, extension, sizeInBytes, uploadTimestamp)
    - Use jqwik framework with minimum 100 iterations
    - Tag: `@Tag("Feature: upload-service-separation, Property 8: SQS Message Serialization Round-Trip")`

  - [ ]* 9.4 Write property test for SQS failure resilience
    - **Property 9: SQS Failure Resilience**
    - **Validates: Requirements 6.5, 11.4**
    - Mock SQS to fail (even after retries)
    - Upload valid video file
    - Verify upload still succeeds with HTTP 201 and video details
    - Verify SQS error is logged
    - Use jqwik framework with minimum 100 iterations
    - Tag: `@Tag("Feature: upload-service-separation, Property 9: SQS Failure Resilience")`

  - [ ]* 9.5 Write property test for error logging
    - **Property 10: Error Logging**
    - **Validates: Requirements 11.1**
    - Generate various exception scenarios (validation, storage, messaging)
    - Verify each exception is logged with appropriate context before returning error response
    - Use jqwik framework with minimum 100 iterations
    - Tag: `@Tag("Feature: upload-service-separation, Property 10: Error Logging")`

- [ ] 10. Update and verify unit tests
  - [ ]* 10.1 Update unit tests for VideoController
    - Update package imports to com.videoupload
    - Remove tests for /slice endpoint
    - Verify upload endpoint tests still pass
    - Add test for backward compatibility of upload endpoint
    - _Requirements: 3.1-3.7, 7.6_

  - [ ]* 10.2 Update unit tests for UploadVideoUseCaseImpl
    - Update package imports to com.videoupload
    - Verify workflow orchestration tests pass
    - Verify SQS failure resilience test passes
    - _Requirements: 3.3-3.7, 7.7_

  - [ ]* 10.3 Update unit tests for AwsS3StorageAdapter
    - Update package imports to com.videoupload
    - Remove tests for retrieve method
    - Verify retry mechanism tests pass
    - _Requirements: 5.1-5.7, 7.4_

  - [ ]* 10.4 Update unit tests for AwsSqsPublisherAdapter
    - Update package imports to com.videoupload
    - Verify retry mechanism tests pass
    - Verify JSON serialization tests pass
    - _Requirements: 6.1-6.6, 7.5_

  - [ ]* 10.5 Update unit tests for domain classes
    - Update package imports to com.videoupload
    - Verify VideoValidation tests pass
    - Verify VideoMetadata tests pass
    - _Requirements: 4.1-4.5_

  - [ ]* 10.6 Add refactoring verification tests
    - Write test to verify /slice endpoint returns 404
    - Write test to verify SliceVideoUseCase class doesn't exist
    - Write test to verify javacv dependency is not in classpath
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 11. Final checkpoint - Run full test suite and verify deployment readiness
  - Ensure all tests pass (unit, property-based, and integration tests), verify upload functionality works end-to-end, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation and allow for user feedback
- Property tests validate universal correctness properties across randomized inputs
- Unit tests validate specific examples, edge cases, and integration points
- The refactoring maintains hexagonal architecture and backward compatibility for upload API consumers
- All AWS integrations (S3, SQS) remain functional with retry mechanisms intact
