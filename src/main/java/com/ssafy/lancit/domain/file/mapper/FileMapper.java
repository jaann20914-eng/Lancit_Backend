package com.ssafy.lancit.domain.file.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.global.enums.FileParentType;

@Mapper
public interface FileMapper {

    // TODO 지원: FileMapper.xml → SELECT * FROM file WHERE file_id = #{fileId}
    FileDTO findById(int fileId);

    // TODO 지원: FileMapper.xml → SELECT * FROM file
    //            WHERE parent_type = #{parentType} AND parent_id = #{parentId}
    List<FileDTO> findByParent(@Param("parentType") FileParentType parentType,
                               @Param("parentId") int parentId);

    // TODO 지원: FileMapper.xml → INSERT INTO file (user_email, company_email, sys_name, ...)
    //            useGeneratedKeys="true" keyProperty="fileId" 반드시 추가
    //            → insert 후 dto.getFileId() 로 생성된 PK 꺼낼 수 있음
    void insert(FileDTO dto);

    // TODO 지원: FileMapper.xml → DELETE FROM file WHERE file_id = #{fileId}
    void delete(int fileId);

    // TODO 지원: FileMapper.xml → DELETE FROM file
    //            WHERE parent_type = #{parentType} AND parent_id = #{parentId}
    void deleteByParent(@Param("parentType") FileParentType parentType,
                        @Param("parentId") int parentId);
}