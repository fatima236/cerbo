package com.example.cerbo.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/evaluateur")
public class EvaluateurController {

    @GetMapping("/home")
    public String home() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return "evaluateur_home"; // Vue gérée par React
    }
}
