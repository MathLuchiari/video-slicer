package com.videoslicer.video_slicer.adapters.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoslicer.video_slicer.application.ports.SQSPublisherService;
import com.videoslicer.video_slicer.domain.video.VideoMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

@Component
public class AwsSqsPublisherAdapter implements SQSPublisherService {
    
    private static final Logger logger = LoggerFactory.getLogger(AwsSqsPublisherAdapter.class);
    private static final int MAX_RETRIES = 3;
    
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;
    
    public AwsSqsPublisherAdapter(SqsClient sqsClient,
                                 @Value("${aws.sqs.queue-url}") String queueUrl,
                                 ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void publish(VideoMetadata videoMetadata) throws PublishException {
        String messageBody = createMessageBody(videoMetadata);
        
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.info("Tentativa {} de {} para publicar mensagem na SQS para vídeo: {}", 
                          attempt, MAX_RETRIES, videoMetadata.getVideoId());
                
                SendMessageRequest sendRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();
                
                sqsClient.sendMessage(sendRequest);
                logger.info("Mensagem publicada na SQS para vídeo: {}", 
                          videoMetadata.getVideoId());
                return;
                
            } catch (Exception e) {
                lastException = e;
                logger.warn("Tentativa {} de {} falhou ao publicar na SQS: {}", 
                          attempt, MAX_RETRIES, e.getMessage(), e);
                
                if (attempt < MAX_RETRIES) {
                    long backoffMillis = (long) Math.pow(2, attempt) * 1000;
                    logger.info("Aguardando {}ms antes da próxima tentativa", backoffMillis);
                    try {
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new PublishException("Publicação interrompida", ie);
                    }
                }
            }
        }
        
        logger.error("Falha ao publicar mensagem na SQS após {} tentativas", MAX_RETRIES);
        throw new PublishException(
            "Falha ao publicar mensagem na SQS após " + MAX_RETRIES + " tentativas", 
            lastException
        );
    }
    
    private String createMessageBody(VideoMetadata metadata) throws PublishException {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("videoId", metadata.getVideoId().toString());
            message.put("fileName", metadata.getFileName());
            message.put("extension", metadata.getExtension());
            message.put("sizeInBytes", metadata.getSizeInBytes());
            message.put("uploadTimestamp", metadata.getUploadTimestamp().toString());
            
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            logger.error("Erro ao criar corpo da mensagem JSON", e);
            throw new PublishException("Erro ao criar corpo da mensagem JSON", e);
        }
    }
}
