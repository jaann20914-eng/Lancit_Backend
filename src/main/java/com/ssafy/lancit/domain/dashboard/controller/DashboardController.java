package com.ssafy.lancit.domain.dashboard.controller;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.dashboard.dto.CompanyDashboardResponse;
import com.ssafy.lancit.domain.dashboard.dto.FreelancerDashboardResponse;
import com.ssafy.lancit.domain.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "역할별 대시보드 집계 API")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "프리랜서 대시보드 조회")
    @GetMapping("/freelancer")
    public ResponseEntity<ApiResponse<FreelancerDashboardResponse>> getFreelancerDashboard() {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getFreelancerDashboard(email, role)));
    }

    @Operation(summary = "회사 대시보드 조회")
    @GetMapping("/company")
    public ResponseEntity<ApiResponse<CompanyDashboardResponse>> getCompanyDashboard() {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getCompanyDashboard(email, role)));
    }
}
