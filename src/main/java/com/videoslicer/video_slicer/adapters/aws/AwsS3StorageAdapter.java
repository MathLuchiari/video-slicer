package com.videoslicer.video_slicer.adapters.aws;

import com.videoslicer.video_slicer.application.ports.S3StorageService;
import com.videoslicer.video_slicer.domain.video.VideoMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Component
public class AwsS3StorageAdapter implements S3StorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(AwsS3StorageAdapter.class);
    private static final int MAX_RETRIES = 3;
    
    private final S3Client s3Client;
    private final String bucketName;
    private final String folderPrefix;
    
    public AwsS3StorageAdapter(S3Client s3Client,
                              @Value("${aws.s3.bucket-name}") String bucketName,
                              @Value("${aws.s3.folder-prefix:videos/}") String folderPrefix) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.folderPrefix = folderPrefix;
    }
    
    @Override
    public String store(VideoMetadata videoMetadata, InputStream inputStream) 
            throws StorageException {
        String key = folderPrefix + videoMetadata.getS3Key();
        
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info("Tentativa {} de {} para armazenar vídeo no S3: {}", 
                          attempt, MAX_RETRIES, key);
                
                PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(getContentType(videoMetadata.getExtension()))
                    .contentLength(videoMetadata.getSizeInBytes())
                    .build();
                
                s3Client.putObject(putRequest, RequestBody.fromInputStream(
                    inputStream, videoMetadata.getSizeInBytes()));
                
                String url = String.format("https://%s.s3.amazonaws.com/%s", 
                                         bucketName, key);
                logger.info("Vídeo armazenado com sucesso no S3: {}", url);
                return url;
                
            } catch (Exception e) {
                lastException = e;
                logger.warn("Tentativa {} de {} falhou ao armazenar no S3: {}", 
                          attempt, MAX_RETRIES, e.getMessage(), e);
                
                if (attempt < MAX_RETRIES) {
                    long backoffMillis = (long) Math.pow(2, attempt) * 1000;
                    logger.info("Aguardando {}ms antes da próxima tentativa", backoffMillis);
                    try {
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new StorageException("Upload interrompido", ie);
                    }
                }
            }
        }
        
        logger.error("Falha ao armazenar vídeo no S3 após {} tentativas", MAX_RETRIES);
        throw new StorageException(
            "Falha ao armazenar vídeo no S3 após " + MAX_RETRIES + " tentativas", 
            lastException
        );
    }
    
    @Override
    public InputStream retrieve(String videoId, String extension) 
            throws StorageException {
        String key = folderPrefix + videoId + "." + extension;
        
        try {
            logger.info("Recuperando vídeo do S3: {}", key);
            
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            
            InputStream result = s3Client.getObject(getRequest);
            logger.info("Vídeo recuperado com sucesso do S3: {}", key);
            return result;
            
        } catch (Exception e) {
            logger.error("Erro ao recuperar vídeo do S3: {}", key, e);
            throw new StorageException("Erro ao recuperar vídeo do S3: " + key, e);
        }
    }
    
    private String getContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            case "mkv" -> "video/x-matroska";
            case "webm" -> "video/webm";
            default -> "application/octet-stream";
        };
    }
}
