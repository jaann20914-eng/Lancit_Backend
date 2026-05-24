package com.ssafy.lancit.domain.file.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.global.enums.FileParentType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {
 
    private final FileMapper fileMapper;
    private final GcsService gcsService;
    private final ApplicationEventPublisher eventPublisher;
 
    /** 파일 업로드 (GCS + DB)
     *  - GCS 업로드 먼저 → DB 저장
     *  - 실패 시 GCS 파일 수동 롤백 처리 */
    @Transactional
    public FileDTO upload(MultipartFile file, FileParentType parentType, Integer parentId) {
        // TODO 지원: role 분기 → userEmail or companyEmail 세팅
        //   gcsService.upload(file) → uploadPath 획득
        //   fileMapper.insert(dto) → fileId 반환
        return null;
    }
 
    /** 단건 파일 삭제 (DB + GCS 이벤트) */
    @Transactional
    public void delete(int fileId) {
        // TODO 지원: fileMapper.findById(fileId) → FileDeleteEvent 발행 → fileMapper.delete(fileId)
    }
 
    /** parentId 기준 파일 목록 조회 (포트폴리오 결과물 등) */
    public List<FileDTO> findByParent(FileParentType parentType, int parentId) {
        // TODO 지원: fileMapper.findByParent(parentType, parentId)
        return null;
    }
 
    /** parentId 기준 파일 DB 삭제 + GCS 이벤트 (포트폴리오 삭제 시 호출) */
    @Transactional
    public void deleteByParent(FileParentType parentType, int parentId) {
        // TODO 지원: findByParent → 각 파일 FileDeleteEvent 발행 → fileMapper.deleteByParent
    }
}