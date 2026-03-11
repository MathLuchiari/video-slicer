package com.videoslicer.video_slicer.application.usecases.impl;

import com.videoslicer.video_slicer.application.ports.S3StorageService;
import com.videoslicer.video_slicer.application.ports.SQSPublisherService;
import com.videoslicer.video_slicer.application.usecases.UploadVideoUseCase;
import com.videoslicer.video_slicer.domain.video.VideoMetadata;
import com.videoslicer.video_slicer.domain.video.VideoValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
public class UploadVideoUseCaseImpl implements UploadVideoUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(UploadVideoUseCaseImpl.class);
    
    private final S3StorageService s3StorageService;
    private final SQSPublisherService sqsPublisherService;
    
    public UploadVideoUseCaseImpl(S3StorageService s3StorageService,
                                 SQSPublisherService sqsPublisherService) {
        this.s3StorageService = s3StorageService;
        this.sqsPublisherService = sqsPublisherService;
    }
    
    @Override
    public UploadResult execute(MultipartFile file) {
        try {
            // Validação
            var validation = VideoValidation.validate(
                file.getOriginalFilename(), 
                file.getSize()
            );
            
            if (!validation.isValid()) {
                return new UploadResult(false, validation.getErrorMessage());
            }
            
            // Geração de metadados
            UUID videoId = UUID.randomUUID();
            String extension = extractExtension(file.getOriginalFilename());
            VideoMetadata metadata = new VideoMetadata(
                videoId,
                file.getOriginalFilename(),
                extension,
                file.getSize(),
                Instant.now()
            );
            
            // Armazenamento no S3
            String videoUrl = s3StorageService.store(metadata, file.getInputStream());
            logger.info("Vídeo armazenado no S3: {}", videoUrl);
            
            // Publicação na fila SQS
            try {
                sqsPublisherService.publish(metadata);
                logger.info("Mensagem publicada na SQS para vídeo: {}", videoId);
            } catch (Exception e) {
                // Falha na publicação não impede sucesso do upload
                logger.error("Erro ao publicar mensagem na SQS para vídeo: {}", videoId, e);
            }
            
            String fileName = metadata.getS3Key();
            return new UploadResult(true, fileName, videoId, videoUrl, 
                                  "Vídeo carregado com sucesso!");
            
        } catch (Exception e) {
            logger.error("Erro ao fazer upload do vídeo", e);
            return new UploadResult(false, "Erro ao carregar o arquivo: " + e.getMessage());
        }
    }
    
    private String extractExtension(String fileName) {
        if (fileName == null) return "";
        int i = fileName.lastIndexOf('.');
        if (i < 0) return "";
        return fileName.substring(i + 1);
    }
}
