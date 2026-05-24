package com.ssafy.lancit.domain.file.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.global.enums.FileParentType;

@Mapper
public interface FileMapper {
    FileDTO findById(int fileId);
    List<FileDTO> findByParent(@Param("parentType") FileParentType parentType, @Param("parentId") int parentId);
    void insert(FileDTO dto);
    void delete(int fileId);
    void deleteByParent(@Param("parentType") FileParentType parentType, @Param("parentId") int parentId);
}
 
