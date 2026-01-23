package com.videoslicer.video_slicer.domain.user;

import java.util.Objects;
import java.util.regex.Pattern;

public class Email {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    private String address;

    public Email(String address) {
        Objects.requireNonNull(address, "Email address cannot be null");
        
        String normalizedAddress = address.trim();

        if (!EMAIL_PATTERN.matcher(normalizedAddress).matches()) {
            throw new IllegalArgumentException("Invalid email address format: " + normalizedAddress);
        }

        this.address = normalizedAddress;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return address.equals(email.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public String toString() {
        return address;
    }
}
