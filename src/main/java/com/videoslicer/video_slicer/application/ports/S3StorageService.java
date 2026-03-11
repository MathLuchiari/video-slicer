package com.videoslicer.video_slicer.application.ports;

import com.videoslicer.video_slicer.domain.video.VideoMetadata;
import java.io.InputStream;

public interface S3StorageService {
    
    /**
     * Armazena um vídeo no S3
     * @param videoMetadata Metadados do vídeo
     * @param inputStream Stream do arquivo de vídeo
     * @return URL completa do vídeo armazenado no S3
     * @throws StorageException se houver erro no armazenamento
     */
    String store(VideoMetadata videoMetadata, InputStream inputStream) 
        throws StorageException;
    
    /**
     * Recupera um vídeo do S3 como InputStream
     * @param videoId ID do vídeo
     * @param extension Extensão do arquivo
     * @return InputStream do vídeo
     * @throws StorageException se o vídeo não for encontrado ou houver erro
     */
    InputStream retrieve(String videoId, String extension) 
        throws StorageException;
    
    class StorageException extends Exception {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
