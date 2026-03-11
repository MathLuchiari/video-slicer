package com.videoslicer.video_slicer.domain.video;

import java.util.Set;

public class VideoValidation {
    private static final Set<String> SUPPORTED_EXTENSIONS = 
        Set.of("mp4", "avi", "mov", "mkv", "webm");
    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB
    
    public static ValidationResult validate(String fileName, long fileSize) {
        if (fileName == null || fileName.isBlank()) {
            return ValidationResult.invalid("Nome do arquivo não pode ser vazio");
        }
        
        String extension = extractExtension(fileName);
        if (!SUPPORTED_EXTENSIONS.contains(extension.toLowerCase())) {
            return ValidationResult.invalid(
                "Extensão não suportada. Formatos aceitos: " + SUPPORTED_EXTENSIONS
            );
        }
        
        if (fileSize <= 0) {
            return ValidationResult.invalid("Arquivo vazio");
        }
        
        if (fileSize > MAX_FILE_SIZE) {
            return ValidationResult.invalid(
                "Arquivo excede o tamanho máximo de 500MB"
            );
        }
        
        return ValidationResult.valid();
    }
    
    private static String extractExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0) return "";
        return fileName.substring(lastDot + 1);
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}
