package com.videoslicer.video_slicer.application.usecases.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.videoslicer.video_slicer.application.usecases.UploadVideoUseCase;

@Service
public class UploadVideoUseCaseImpl implements UploadVideoUseCase {

    private final String basePath = "src/main/videos/";

    @Override
    public UploadResult execute(MultipartFile file) {
        try {
            File dir = new File(basePath);
            if (!dir.exists()) dir.mkdirs();

            String extension = extrairExtensao(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString() + "." + extension;
            String caminho = basePath + fileName;

            Files.copy(file.getInputStream(), Path.of(caminho), StandardCopyOption.REPLACE_EXISTING);
            return new UploadResult(true, fileName, "Arquivo carregado com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
            return new UploadResult(false, null, "Erro ao carregar o arquivo!");
        }
    }

    private String extrairExtensao(String fileName) {
        if (fileName == null) return "";
        int i = fileName.lastIndexOf('.');
        if (i < 0) return "";
        return fileName.substring(i + 1);
    }
}
