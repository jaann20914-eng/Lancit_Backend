package com.ssafy.lancit.domain.contract.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.ssafy.lancit.global.enums.Weekday;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractDocumentDTO {

    private Integer contractId;

    // 갑
    private String partyA;
    private String representativeName;
    private String companyAddress;

    // 을
    private String partyB;
    private LocalDate freelancerBirthDate;
    private String freelancerAddress;

    // 근무 정보
    private String workLocation;
    private String workDescription;

    private List<Weekday> workDays;

    private LocalTime workStartTime;
    private LocalTime workEndTime;

    private LocalTime breakTime;

    // 계약 기간
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;

    // 급여
    private Integer monthlyWage;

    private Integer basePay;
    private LocalTime basePayBasis;

    private Integer overtimePay;
    private LocalTime overtimePayBasis;

    private Integer holidayPay;
    private LocalTime holidayPayBasis;

    private Integer mealAllowance;

    private Integer totalWage;

    // 서명
    private Integer representativeSignFileId;

    private Integer freelancerSignFileId;

    private LocalDateTime contractWrittenAt;

    private LocalDateTime confirmedAt;
}
