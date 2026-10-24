package com.conduit.backend.identity.domain;

public final class User {

    private final Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String bio;
    private String image;

    private User(Long id, String username, String email, String passwordHash, String bio, String image) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.bio = bio;
        this.image = image;
    }

    public static User register(String username, String email, String passwordHash) {
        return new User(null, username, email, passwordHash, null, null);
    }

    public static User restore(Long id, String username, String email, String passwordHash, String bio, String image) {
        return new User(id, username, email, passwordHash, bio, image);
    }

    public void rename(String username) {
        this.username = username;
    }

    public void changeEmail(String email) {
        this.email = email;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void changeBio(String bio) {
        this.bio = blankToNull(bio);
    }

    public void changeImage(String image) {
        this.image = blankToNull(image);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getBio() {
        return bio;
    }

    public String getImage() {
        return image;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
