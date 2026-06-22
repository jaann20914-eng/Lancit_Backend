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

    // 갑(회사)
    private String partyA;
    private String representativeName;
    private String companyAddress;

    // 을(프리랜서)
    private String partyB;
    private LocalDate freelancerBirthDate;
    private String freelancerAddress;
    private String confirmSignerName;
    private String privacySignerName;

    // 근무 정보
    private String workLocation;
    private String workDescription;
    private String workDays; // List<Weekday> → String
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private LocalTime breakTimeStart;
    private LocalTime breakTimeEnd;

    // 계약 기간
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;

    // 급여
    private Integer monthlyWage;
    private Integer basePay;
    //private String basePayBasis;
    private Integer overtimePay;
   // private LocalTime overtimePayBasis;
    private Integer holidayPay;
    //private LocalTime holidayPayBasis;
    private Integer mealAllowance;
    private Integer totalWage;

    // 서명
    private Integer representativeSignFileId;
    private Integer contractSignFileId;
    private Integer confirmSignFileId;
    private Integer privacySignFileId;

    // 작성/확정
    private LocalDate contractWrittenAt;
    private LocalDateTime confirmedAt;
    
	private Integer basePayBasisMinutes;
	private Integer overtimePayBasisMinutes;
	private Integer holidayPayBasisMinutes;
}