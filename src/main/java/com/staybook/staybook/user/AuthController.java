package com.staybook.staybook.user;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final UserService userService;
    public AuthController(UserService userService) { this.userService = userService; }
    @GetMapping("/login") public String login() { return "public/login"; }
    @GetMapping("/register") public String register(Model model) {
        model.addAttribute("form", new RegistrationForm()); return "public/register";
    }
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult result, Model model) {
        if (!result.hasErrors()) {
            try { userService.register(form); return "redirect:/login?registered"; }
            catch (IllegalArgumentException ex) { model.addAttribute("formError", ex.getMessage()); }
        }
        return "public/register";
    }
}
