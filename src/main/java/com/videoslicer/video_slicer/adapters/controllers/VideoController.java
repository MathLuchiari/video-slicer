package com.videoslicer.video_slicer.adapters.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.videoslicer.video_slicer.application.usecases.UploadVideoUseCase;
import com.videoslicer.video_slicer.application.usecases.SliceVideoUseCase;

@Validated
@RestController
@RequestMapping("/api/video")
public class VideoController {
    private final UploadVideoUseCase uploadUseCase;
    private final SliceVideoUseCase sliceUseCase;

    public VideoController(UploadVideoUseCase uploadUseCase, SliceVideoUseCase sliceUseCase) {
        this.uploadUseCase = uploadUseCase;
        this.sliceUseCase = sliceUseCase;
    }
    //@RequestMapping( method=RequestMethod.GET)
    // @GetMapping
    // public List<CourseDTO> list() {
    //     return courseService.list();
    // }
    @PostMapping("/upload")
    @ResponseStatus( HttpStatus.CREATED )
    public ResponseEntity<?> upload( @RequestParam MultipartFile file) {
        var result = uploadUseCase.execute(file);
        if (result.success) {
            return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of("message", result.message, "content", java.util.Map.of("fileName", result.fileName)));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("message", result.message));
    }

    @GetMapping("/slice")
    @ResponseStatus( HttpStatus.OK )
    public ResponseEntity<?> slice( @RequestParam String fileName, @RequestParam int timeInterval) {
        var result = sliceUseCase.execute(fileName, timeInterval);
        if (result.success) {
            return ResponseEntity.ok(java.util.Map.of("message", result.message, "content", java.util.Map.of("outputPrefix", result.outputPrefix)));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("message", result.message));
    }
}