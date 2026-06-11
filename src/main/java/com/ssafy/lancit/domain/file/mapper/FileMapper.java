package com.ssafy.lancit.domain.file.mapper;

import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.global.enums.FileParentType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileMapper {

    // 아이디로 filedto 찾기
    FileDTO findById(int fileId);

    // parentId 기준 파일 목록 조회
    List<FileDTO> findByParent(@Param("parentType") FileParentType parentType,
                               @Param("parentId") int parentId);

    // UserService.delete() 에서 탈퇴 시 파일 정리용
    List<FileDTO> findByUserEmail(String userEmail);

    // CompanyService.delete() 에서 탈퇴 시 파일 정리용
    // SELECT * FROM file WHERE company_email = #{companyEmail}
    List<FileDTO> findByCompanyEmail(String companyEmail);

    // 파일 dto 삽입하기
    void insert(FileDTO dto);

    //파일 dto 삭제하기 + 부모아이디로 삭제도 여기로 개별 호출함
    void delete(int fileId);

    // OwnerCheckAspect 에서 소유자 검증
    String findOwnerEmailById(int fileId);
    
    // 특정 유저의 TEMP 파일 전체 조회
    List<FileDTO> findTempByEmail(@Param("email") String email);
    
    // TEMP → PROFILE 등 parentType 변경
    void updateParentType(@Param("fileId") int fileId,
                          @Param("parentType") FileParentType parentType);

    // 파일을 특정 도메인 부모에 연결
    void updateParent(@Param("fileId") int fileId,
                      @Param("parentType") FileParentType parentType,
                      @Param("parentId") Integer parentId);

    // 부모 연결만 해제 (GCS/파일 row 삭제 없음)
    void detachByParent(@Param("parentType") FileParentType parentType,
                        @Param("parentId") int parentId);

    void detach(int fileId);

    //GCS 폴더경로 바꾸기
    void updatePath(@Param("fileId") Integer fileId,@Param("uploadPath") String uploadPath);

}
