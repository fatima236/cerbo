package com.example.cerbo.controller;



import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/home")
    public String home() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return "home"; // Cette vue sera gérée par React
    }
}