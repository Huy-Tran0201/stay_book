package com.staybook.staybook.user;

import jakarta.validation.constraints.*;

public class RegistrationForm {
    @NotBlank private String fullName;
    @NotBlank @Email private String email;
    @Pattern(regexp = "0\\d{9}", message = "{validation.phone}") private String phone;
    @NotBlank @Size(min = 8, message = "{validation.password}") private String password;
    @NotBlank private String confirmPassword;
    public String getFullName() { return fullName; } public void setFullName(String v) { fullName = v; }
    public String getEmail() { return email; } public void setEmail(String v) { email = v; }
    public String getPhone() { return phone; } public void setPhone(String v) { phone = v; }
    public String getPassword() { return password; } public void setPassword(String v) { password = v; }
    public String getConfirmPassword() { return confirmPassword; } public void setConfirmPassword(String v) { confirmPassword = v; }
}
