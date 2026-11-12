package com.pethop.admin;

import com.pethop.admin.dto.AdminMetricsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/admin/metrics")
public class AdminMetricsController { private final AdminMetricsService service; public AdminMetricsController(AdminMetricsService service){this.service=service;} @GetMapping AdminMetricsResponse get(){return service.get();} }
