package com.ssafy.lancit.domain.contract.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.Weekday;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContractDTO {
    private int contractId;
    private String companyEmail;
    private String freelancerEmail;
    private ContractStatus status;
 
    // 계약서 내용
    private String partyA;
    private String partyB;
    private LocalDateTime contractStartAt;
    private LocalDateTime contractEndAt;
    private String workLocation;
    private String workDescription;
    private List<Weekday> workDays;     // DB: 콤마 구분 문자열
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private LocalTime breakTime;
 
    // 급여
    private int monthlyWage;
    private int basePay;
    private LocalTime basePayBasis;
    private int overtimePay;
    private LocalTime overtimePayBasis;
    private int holidayPay;
    private LocalTime holidayPayBasis;
    private int mealAllowance;
    private int totalWage;
 
    // 서명
    private LocalDateTime contractWrittenAt;
    private String representativeName;
    private String companyAddress;
    private Integer representativeSignFileId;
    private Integer freelancerSignFileId;
    private LocalDate freelancerBirthDate;
    private String freelancerAddress;
    private Integer confirmSignFileId;
}
 