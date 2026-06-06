package com.ssafy.lancit.domain.calendar.task.mapper;

import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.global.enums.OwnerType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TaskMapper {

    // 일정 목록 조회 - email + ownerType 으로 내 일정만 필터, 기간/categoryId 조건은 선택
    List<TaskDTO> findByCondition(@Param("email") String email,
                                  @Param("ownerType") OwnerType ownerType,
                                  @Param("rangeStart") LocalDateTime rangeStart,
                                  @Param("rangeEndExclusive") LocalDateTime rangeEndExclusive,
                                  @Param("categoryId") Integer categoryId);

    // 일정 상세 조회
    TaskDTO findById(@Param("taskId") int taskId);

    TaskDTO findByIdAndOwner(@Param("taskId") int taskId,
                             @Param("email") String email,
                             @Param("ownerType") OwnerType ownerType);

    boolean existsByIdAndOwner(@Param("taskId") int taskId,
                               @Param("email") String email,
                               @Param("ownerType") OwnerType ownerType);

    // OwnerCheckAspect 에서 소유자 검증용
    String findOwnerEmailById(@Param("taskId") int taskId);

    // 일정 등록
    void insert(TaskDTO dto);

    // 일정 수정 - null 필드 UPDATE 제외 (XML <if test> 처리)
    int update(@Param("taskId") int taskId, @Param("dto") TaskDTO dto);

    // 일정 삭제
    int delete(@Param("taskId") int taskId);

    int deleteByIdAndOwner(@Param("taskId") int taskId,
                           @Param("email") String email,
                           @Param("ownerType") OwnerType ownerType);

    // 회원 탈퇴 시 소유자 기준 전체 삭제
    void deleteByOwner(@Param("email") String email, @Param("ownerType") OwnerType ownerType);

    // 카테고리 삭제 전 Task 존재 여부 확인용 (CategoryService.delete() 에서 사용)
    int countByCategory(@Param("categoryId") int categoryId);

    // 카테고리 삭제 시 연관 일정의 categoryId 일괄 이동
    int updateCategoryByCategory(@Param("categoryId") int categoryId,
                                 @Param("moveToCategoryId") int moveToCategoryId,
                                 @Param("email") String email,
                                 @Param("ownerType") OwnerType ownerType);

    // TaskScheduler 에서 납기일 D-7/3/1/0 알림 대상 조회
    List<TaskDTO> findDeadlineTasks(@Param("daysUntilDeadline") int daysUntilDeadline);
}
