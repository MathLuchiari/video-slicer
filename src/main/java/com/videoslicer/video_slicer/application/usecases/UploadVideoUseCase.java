package com.videoslicer.video_slicer.application.usecases;

import org.springframework.web.multipart.MultipartFile;

public interface UploadVideoUseCase {
    UploadResult execute(MultipartFile file);

    class UploadResult {
        public final boolean success;
        public final String fileName;
        public final String message;

        public UploadResult(boolean success, String fileName, String message) {
            this.success = success;
            this.fileName = fileName;
            this.message = message;
        }
    }
}
