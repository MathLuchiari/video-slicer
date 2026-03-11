package com.videoslicer.video_slicer.application.usecases;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface UploadVideoUseCase {
    UploadResult execute(MultipartFile file);

    class UploadResult {
        public final boolean success;
        public final String fileName;  // Mantido para retrocompatibilidade
        public final UUID videoId;     // Novo campo
        public final String videoUrl;  // Novo campo
        public final String message;

        public UploadResult(boolean success, String fileName, UUID videoId, 
                          String videoUrl, String message) {
            this.success = success;
            this.fileName = fileName;
            this.videoId = videoId;
            this.videoUrl = videoUrl;
            this.message = message;
        }
        
        // Construtor para casos de erro (retrocompatibilidade)
        public UploadResult(boolean success, String message) {
            this(success, null, null, null, message);
        }
    }
}
