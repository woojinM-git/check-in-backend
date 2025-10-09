package com.sist.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class testController {
    
    @GetMapping("/test")
    public String test() {
        System.out.println("test");
        System.out.println("test2");
        System.out.println("test3");
        System.out.println("test3");
        return "test";
    }
}
