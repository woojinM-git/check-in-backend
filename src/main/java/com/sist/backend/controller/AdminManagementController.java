package com.sist.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.service.AdminManagementService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {
    
    @Autowired
    AdminManagementService amService;

    /* 호텔 관리자 대시보드 화면 */
    /* 오늘 체크인, 오늘 체크아웃, 예약 대기, 이번달 매출 */
    /* 최근 예약 현황 */

}
