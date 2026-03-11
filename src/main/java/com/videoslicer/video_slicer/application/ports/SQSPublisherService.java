package com.videoslicer.video_slicer.application.ports;

import com.videoslicer.video_slicer.domain.video.VideoMetadata;

public interface SQSPublisherService {
    
    /**
     * Publica mensagem com metadados do vídeo na fila SQS
     * @param videoMetadata Metadados do vídeo a serem publicados
     * @throws PublishException se houver erro na publicação
     */
    void publish(VideoMetadata videoMetadata) throws PublishException;
    
    class PublishException extends Exception {
        public PublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
