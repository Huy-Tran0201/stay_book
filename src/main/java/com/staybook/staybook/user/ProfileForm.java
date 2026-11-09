package com.staybook.staybook.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileForm {
    private String fullName;
    @Pattern(regexp = "0\\d{9}", message = "{validation.phone}") private String phone;
    private String currentPassword;
    @Size(min = 8, message = "{validation.password}") private String newPassword;
    public String getFullName() { return fullName; } public void setFullName(String v) { fullName = v; }
    public String getPhone() { return phone; } public void setPhone(String v) { phone = v; }
    public String getCurrentPassword() { return currentPassword; } public void setCurrentPassword(String v) { currentPassword = v; }
    public String getNewPassword() { return newPassword; } public void setNewPassword(String v) { newPassword = v; }
}
