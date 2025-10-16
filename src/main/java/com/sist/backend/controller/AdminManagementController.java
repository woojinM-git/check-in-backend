package com.sist.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.service.AdminManagementService;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {
    
    @Autowired
    AdminManagementService amService;

    @RequestMapping("/all")
    public Object findAll() {
        return amService.findAll();
    }

    @RequestMapping("daminIdx")
    public Object findByadminIdx(@RequestParam Integer adminIdx) {
        return amService.findByadminIdx(adminIdx);
    }
}
