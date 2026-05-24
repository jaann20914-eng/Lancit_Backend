package com.ssafy.lancit.domain.company.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.company.dto.CompanyDTO;
import com.ssafy.lancit.domain.company.service.CompanyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {
 
    private final CompanyService companyService;
 
    /** CLI-USER-03 마이페이지 조회 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CompanyDTO>> getMe() {
        // TODO 지원: companyService.getMe(email)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CLI-USER-04 마이페이지 수정 */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMe(@RequestBody CompanyDTO dto) {
        // TODO 지원: companyService.update(dto)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CLI-USER-02 회원 탈퇴 (@ContractGuard → 진행 중 계약 있으면 차단) */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe() {
        // TODO 지원: companyService.delete()
        //   → Category(ownerType=COMPANY), Task(ownerType=COMPANY) 앱 레벨 삭제 후 Company 삭제
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}