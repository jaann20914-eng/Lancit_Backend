package com.ssafy.lancit.domain.file.mapper;

import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.global.enums.FileParentType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileMapper {

    // SELECT * FROM file WHERE file_id = #{fileId}
    FileDTO findById(int fileId);

    // SELECT * FROM file WHERE parent_type = #{parentType} AND parent_id = #{parentId}
    List<FileDTO> findByParent(@Param("parentType") FileParentType parentType,
                               @Param("parentId") int parentId);

    // UserService.delete() 에서 탈퇴 시 파일 정리용
    // SELECT * FROM file WHERE user_email = #{userEmail}
    List<FileDTO> findByUserEmail(String userEmail);

    // CompanyService.delete() 에서 탈퇴 시 파일 정리용
    // SELECT * FROM file WHERE company_email = #{companyEmail}
    List<FileDTO> findByCompanyEmail(String companyEmail);

    // INSERT INTO file - useGeneratedKeys="true" keyProperty="fileId" 필수
    void insert(FileDTO dto);

    // DELETE FROM file WHERE file_id = #{fileId}
    void delete(int fileId);

    // DELETE FROM file WHERE parent_type = #{parentType} AND parent_id = #{parentId}
    void deleteByParent(@Param("parentType") FileParentType parentType,
                        @Param("parentId") int parentId);

    // OwnerCheckAspect 에서 소유자 검증용
    // SELECT COALESCE(user_email, company_email) FROM file WHERE file_id = #{fileId}
    String findOwnerEmailById(int fileId);
}