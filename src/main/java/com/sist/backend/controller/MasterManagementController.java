package com.sist.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.service.MasterManagementService;

@RestController
@RequestMapping("/api/master")
public class MasterManagementController {
    
    @Autowired
    MasterManagementService mmService;
}
