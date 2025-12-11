package com._9.demo.controller; // 或者 com._9.demo.dto;

public class ChangePasswordRequest {
    private String username;
    private String oldPassword;
    private String newPassword;

    // Getters
    public String getUsername() {
        return username;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    // Setters
    public void setUsername(String username) {
        this.username = username;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}