package com.videoslicer.video_slicer.domain.video;

import java.util.UUID;

public class Video {
    private UUID id;
    private String title;
    private String url;

    public Video(UUID id, String title, String url) {
        this.id = id;
        this.title = title;
        this.url = url;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }
}