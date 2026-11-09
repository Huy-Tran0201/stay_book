package com.staybook.staybook.user;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProfileController {
    private final UserService userService;
    public ProfileController(UserService userService) { this.userService = userService; }
    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName());
        ProfileForm form = new ProfileForm(); form.setFullName(user.getFullName()); form.setPhone(user.getPhone());
        model.addAttribute("form", form); return "user/profile";
    }
    @PostMapping("/profile")
    public String update(@Valid @ModelAttribute("form") ProfileForm form, BindingResult result,
                         Principal principal, Model model) {
        if (!result.hasErrors()) {
            try { userService.updateProfile(principal.getName(), form); return "redirect:/profile?saved"; }
            catch (IllegalArgumentException ex) { model.addAttribute("formError", ex.getMessage()); }
        }
        return "user/profile";
    }
}
