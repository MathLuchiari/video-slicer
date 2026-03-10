package com.videoslicer.video_slicer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import com.videoslicer.video_slicer.domain.user.Email;
import com.videoslicer.video_slicer.domain.user.Password;
import com.videoslicer.video_slicer.domain.user.User;

@SpringBootApplication
public class VideoSlicerApplication {

	public static void main(String[] args) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		SpringApplication.run(VideoSlicerApplication.class, args);

		System.out.println("Video Slicer Application Started Successfully!");
	}

}
