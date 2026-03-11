package com.videoslicer.video_slicer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AwsConfigurationValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(AwsConfigurationValidator.class);
    
    @Value("${aws.region:#{null}}")
    private String region;
    
    @Value("${aws.s3.bucket-name:#{null}}")
    private String bucketName;
    
    @Value("${aws.sqs.queue-url:#{null}}")
    private String queueUrl;
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        StringBuilder errors = new StringBuilder();
        
        if (region == null || region.isBlank()) {
            errors.append("- aws.region não configurado\n");
        }
        
        if (bucketName == null || bucketName.isBlank()) {
            errors.append("- aws.s3.bucket-name não configurado\n");
        }
        
        if (queueUrl == null || queueUrl.isBlank()) {
            errors.append("- aws.sqs.queue-url não configurado\n");
        }
        
        if (errors.length() > 0) {
            String errorMessage = "Configurações AWS obrigatórias ausentes:\n" + errors;
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        logger.info("Configurações AWS validadas com sucesso");
        logger.info("Região: {}", region);
        logger.info("Bucket S3: {}", bucketName);
        logger.info("Fila SQS: {}", queueUrl);
    }
}
