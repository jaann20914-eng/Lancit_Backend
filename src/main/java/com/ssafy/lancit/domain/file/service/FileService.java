package com.ssafy.lancit.domain.file.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.GcsSignedUrlUtil;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.event.FileDeleteEvent;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.global.enums.FileParentType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 파일 업로드 / 조회 / 삭제
// ★ Redis: @Cacheable(signedUrl, 6일) / @CacheEvict(삭제 시 자동 제거)
// ★ GCS: 업로드/삭제 처리, 삭제는 FileDeleteEvent → 트랜잭션 커밋 후 실행
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMapper fileMapper;
    private final GcsService gcsService;
    private final GcsSignedUrlUtil gcsSignedUrlUtil;
    private final ApplicationEventPublisher eventPublisher;

    // 파일 업로드 - GCS 먼저 업로드 후 DB 저장
    // GCS 성공 + DB 실패 시 GCS 수동 롤백 처리
    @Transactional
    public FileDTO upload(MultipartFile file, FileParentType parentType,
                          Integer parentId, String email, String role) {
        // TODO 지원 [1]: String sysName = gcsService.upload(file)
        // TODO 지원 [2]: FileDTO dto = new FileDTO()
        //               "USER".equals(role) ? dto.setUserEmail(email) : dto.setCompanyEmail(email)
        //               dto.setSysName(sysName)
        //               dto.setOriName(file.getOriginalFilename())
        //               dto.setParentType(parentType)
        //               dto.setParentId(parentId)
        //               dto.setUploadPath(sysName)
        //               dto.setFileSize((int) file.getSize())
        // TODO 지원 [3]: GCS 성공 후 DB 실패 시 수동 롤백
        //               try { fileMapper.insert(dto) }
        //               catch (Exception e) { gcsService.deleteByPath(sysName); throw e; }
        // TODO 지원 [4]: return dto
        return null;
    }

    // 파일 단건 조회
    public FileDTO findById(int fileId) {
        FileDTO dto = fileMapper.findById(fileId);
        if (dto == null) throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        return dto;
    }

    // Signed URL 조회
    // ★ Redis "signedUrl:{fileId}" 캐싱 TTL 6일
    // 최초 조회 시 GCS 발급 후 Redis 저장, 이후 Redis 에서 즉시 반환
    @Cacheable(value = "signedUrl", key = "#fileId")
    public String getSignedUrl(int fileId) {
        FileDTO dto = findById(fileId);
        return gcsSignedUrlUtil.generateForImage(dto.getUploadPath());
    }

    // 파일 단건 삭제
    // ★ @CacheEvict → Redis Signed URL 캐시 자동 제거
    // ★ FileDeleteEvent → 트랜잭션 커밋 후 GCS 실제 파일 삭제
    @Transactional
    @CacheEvict(value = "signedUrl", key = "#fileId")
    public void delete(int fileId) {
        // TODO 지원 [1]: FileDTO dto = fileMapper.findById(fileId)
        //               null 이면 return (이미 삭제된 경우)
        // TODO 지원 [2]: eventPublisher.publishEvent(new FileDeleteEvent(dto.getUploadPath()))
        // TODO 지원 [3]: fileMapper.delete(fileId)
    }

    // parentId 기준 파일 목록 조회
    public List<FileDTO> findByParent(FileParentType parentType, int parentId) {
        // TODO 지원 [1]: return fileMapper.findByParent(parentType, parentId)
        return null;
    }

    // parentId 기준 파일 전체 삭제
    // ★ delete(fileId) 개별 호출 → 각 파일마다 @CacheEvict + FileDeleteEvent 처리
    //   fileMapper.deleteByParent() 직접 호출하면 캐시/GCS 처리 안 됨
    @Transactional
    public void deleteByParent(FileParentType parentType, int parentId) {
        // TODO 지원 [1]: List<FileDTO> files = findByParent(parentType, parentId)
        // TODO 지원 [2]: files.forEach(file -> delete(file.getFileId()))
        //               → 각 파일마다 @CacheEvict + FileDeleteEvent 발행
    }
}