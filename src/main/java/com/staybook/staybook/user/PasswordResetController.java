package com.staybook.staybook.user;

import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PasswordResetController {
    private final PasswordResetService service;
    public PasswordResetController(PasswordResetService service) { this.service = service; }
    @GetMapping("/forgot-password") public String forgot() { return "public/forgot-password"; }
    @PostMapping("/forgot-password")
    public String forgot(@RequestParam String email) {
        service.request(email); return "redirect:/forgot-password?sent";
    }
    @GetMapping("/reset-password") public String reset(@RequestParam String token, Model model) {
        model.addAttribute("token", token); return "public/reset-password";
    }
    @PostMapping("/reset-password")
    public String reset(@RequestParam String token, @RequestParam @Size(min = 8) String password) {
        return service.reset(token, password) ? "redirect:/login?reset" : "redirect:/reset-password?invalid";
    }
}
