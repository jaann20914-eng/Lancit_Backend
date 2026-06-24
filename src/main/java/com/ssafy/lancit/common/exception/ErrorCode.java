package com.ssafy.lancit.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    
    //회원가입
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "지원하지 않는 회원 유형입니다."),
    

    // 인증
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "메일 발송에 실패했습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "이메일 인증이 필요합니다."),
    
    //사업자 번호 인증
    BUSINESS_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "사업자번호 검증 중 오류가 발생했습니다."),

    // 계약
    CONTRACT_IN_PROGRESS(HttpStatus.CONFLICT, "진행 중인 계약이 존재하여 탈퇴할 수 없습니다."),
    CONTRACT_ALREADY_EXISTS(HttpStatus.CONFLICT, "동일 공고와 프리랜서의 진행 중인 계약이 이미 존재합니다."),
    PDF_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "게약서 pdf 생성에 실패했습니다"),


    // 파일
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    

    // OwnerCheckAspect 에서 사용
    INVALID_RESOURCE_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 리소스 타입입니다."),
	
    // 탈퇴한 회원 체크
    WITHDRAWN_COMPANY(HttpStatus.NOT_FOUND, "탈퇴한 회사 계정입니다"),
    WITHDRAWN_USER(HttpStatus.NOT_FOUND, "탈퇴한 프리랜서 계정입니다"),
    
    //일정
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    INVALID_TASK_PERIOD(HttpStatus.BAD_REQUEST, "시작 시간이 종료 시간보다 늦을 수 없습니다."),
    CATEGORY_HAS_TASKS(HttpStatus.CONFLICT, "카테고리에 일정이 존재합니다. 먼저 삭제하거나 이동해주세요."),
    INVALID_CATEGORY_MOVE(HttpStatus.BAD_REQUEST, "삭제 대상 카테고리와 이동 대상 카테고리가 같을 수 없습니다."),

    // 포트폴리오/프로젝트
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    FREELANCER_ONLY(HttpStatus.FORBIDDEN, "프리랜서 계정만 이용할 수 있습니다."),
    INVALID_PORTFOLIO_PERIOD(HttpStatus.BAD_REQUEST, "프로젝트 시작일이 종료일보다 늦을 수 없습니다."),
    INVALID_PORTFOLIO_CATEGORY(HttpStatus.BAD_REQUEST, "허용되지 않은 프로젝트 카테고리입니다."),
    PORTFOLIO_PROFILE_INTRO_TOO_LONG(HttpStatus.BAD_REQUEST, "포트폴리오 한 줄 소개는 30자 이하로 입력해주세요."),
    DELETED_PORTFOLIO(HttpStatus.GONE, "삭제된 프로젝트입니다."),

    // 공고
    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "공고를 찾을 수 없습니다."),
    RECRUITMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "공고에 접근할 권한이 없습니다."),
    RECRUITMENT_COMPANY_ONLY(HttpStatus.FORBIDDEN, "회사 계정만 공고를 관리할 수 있습니다."),
    INVALID_RECRUITMENT_CATEGORY(HttpStatus.BAD_REQUEST, "허용되지 않은 공고 카테고리입니다."),
    INVALID_RECRUITMENT_STATUS(HttpStatus.BAD_REQUEST, "허용되지 않은 공고 상태입니다."),
    INVALID_RECRUITMENT_TAB(HttpStatus.BAD_REQUEST, "허용되지 않은 공고 목록 탭입니다."),
    INVALID_RECRUITMENT_PERIOD(HttpStatus.BAD_REQUEST, "공고 기간이 올바르지 않습니다."),
    RECRUITMENT_HAS_ACTIVE_APPLICATIONS(HttpStatus.CONFLICT, "지원자가 있는 공고는 수정하거나 삭제할 수 없습니다."),
    RECRUITMENT_NOT_OPEN(HttpStatus.CONFLICT, "지원 가능한 공고가 아닙니다."),

    // 공고 지원
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "지원 내역을 찾을 수 없습니다."),
    APPLICATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 지원한 공고입니다."),
    APPLICATION_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 지원입니다."),
    APPLICATION_ALREADY_VIEWED(HttpStatus.CONFLICT, "회사가 열람한 지원은 수정하거나 취소할 수 없습니다."),
    INVALID_APPLICATION_STATUS(HttpStatus.CONFLICT, "현재 상태에서는 지원을 수정하거나 취소할 수 없습니다."),
    INVALID_APPLICATION_STATUS_CHANGE(HttpStatus.CONFLICT, "현재 상태에서는 지원을 수락하거나 거절할 수 없습니다."),
    INVALID_APPLICATION_STATUS_VALUE(HttpStatus.BAD_REQUEST, "지원 상태는 ACCEPTED 또는 REJECTED만 가능합니다."),
    INVALID_APPLICATION_PORTFOLIO(HttpStatus.BAD_REQUEST, "지원에 사용할 수 없는 포트폴리오입니다."),

    // 외부 공고
    EXTERNAL_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "외부 공고를 찾을 수 없습니다."),
    SEOUL_API_KEY_MISSING(HttpStatus.BAD_REQUEST, "서울시 API 키가 설정되지 않았습니다.");
	
	
    // TODO 영은: 기능 구현 중 필요한 에러코드 생기면 여기에 추가
    // TODO 지원: 기능 구현 중 필요한 에러코드 생기면 여기에 추가
    //   예) CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다.")
    //       TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다.")
    //       PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "제안서를 찾을 수 없습니다.")
    //       CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "계약서를 찾을 수 없습니다.")

    private final HttpStatus status;
    private final String message;
}
