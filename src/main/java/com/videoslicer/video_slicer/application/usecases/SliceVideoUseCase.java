package com.videoslicer.video_slicer.application.usecases;

public interface SliceVideoUseCase {
    SliceResult execute(String fileName, int timeIntervalInSeconds);

    class SliceResult {
        public final boolean success;
        public final String message;
        public final String outputPrefix;

        public SliceResult(boolean success, String message, String outputPrefix) {
            this.success = success;
            this.message = message;
            this.outputPrefix = outputPrefix;
        }
    }
}
