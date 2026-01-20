package com.videoslicer.video_slicer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VideoSlicerApplication {

	public static void main(String[] args) {
		SpringApplication.run(VideoSlicerApplication.class, args);

		System.out.println("Video Slicer Application Started Successfully!");
	}

}
