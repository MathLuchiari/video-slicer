package com.videoslicer.video_slicer.domain.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Password {
    private byte[] value;

    public Password(String value) throws NoSuchAlgorithmException {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        MessageDigest algorithm = MessageDigest.getInstance("MD5");
        byte[] messageDigest = algorithm.digest(value.getBytes(StandardCharsets.UTF_8));

        this.value = messageDigest;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public boolean validate(byte[] input) {
        return java.util.Arrays.equals(this.value, input);
    }
}