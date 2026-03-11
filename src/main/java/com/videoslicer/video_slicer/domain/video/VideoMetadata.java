package com.videoslicer.video_slicer.domain.video;

import java.time.Instant;
import java.util.UUID;

public class VideoMetadata {
    private final UUID videoId;
    private final String fileName;
    private final String extension;
    private final long sizeInBytes;
    private final Instant uploadTimestamp;
    
    public VideoMetadata(UUID videoId, String fileName, String extension, 
                        long sizeInBytes, Instant uploadTimestamp) {
        this.videoId = videoId;
        this.fileName = fileName;
        this.extension = extension;
        this.sizeInBytes = sizeInBytes;
        this.uploadTimestamp = uploadTimestamp;
    }
    
    // Getters
    public UUID getVideoId() { 
        return videoId; 
    }
    
    public String getFileName() { 
        return fileName; 
    }
    
    public String getExtension() { 
        return extension; 
    }
    
    public long getSizeInBytes() { 
        return sizeInBytes; 
    }
    
    public Instant getUploadTimestamp() { 
        return uploadTimestamp; 
    }
    
    public String getS3Key() {
        return videoId.toString() + "." + extension;
    }
}
