package com.staybook.staybook.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaffController {
    @GetMapping("/staff/today")
    public String today() { return "staff/today"; }
}
