package com.ssafy.lancit.domain.file.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.event.FileDeleteEvent;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.global.enums.FileParentType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMapper fileMapper;
    private final GcsService gcsService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 파일 업로드 (GCS 먼저 → DB 저장)
     * GCS 성공 후 DB 실패 시 GCS 파일 수동 롤백 필요
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 이메일 꺼내기
     * TODO 지원 [2]: SecurityUtil.getCurrentRole() 로 role 꺼내기
     *               - "USER"    → dto.setUserEmail(email)
     *               - "COMPANY" → dto.setCompanyEmail(email)
     * TODO 지원 [3]: sysName = gcsService.upload(file) 호출
     * TODO 지원 [4]: publicUrl = gcsService.getPublicUrl(sysName) 호출
     * TODO 지원 [5]: FileDTO 세팅
     *               - sysName, oriName(file.getOriginalFilename())
     *               - parentType, parentId
     *               - uploadPath = sysName (GCS 삭제 시 재사용)
     *               - publicUrl
     *               - fileSize = (int) file.getSize()
     * TODO 지원 [6]: fileMapper.insert(dto) 호출
     *               - insert 후 dto.getFileId() 로 생성된 PK 확인 (useGeneratedKeys 설정 필요)
     * TODO 지원 [7]: GCS 성공 후 DB 실패 시 수동 롤백
     *               try { fileMapper.insert(dto) }
     *               catch (Exception e) {
     *                   gcsService storage.delete(sysName) 직접 호출
     *                   throw e;
     *               }
     * TODO 지원 [8]: 완성된 dto 반환
     */
    @Transactional
    public FileDTO upload(MultipartFile file, FileParentType parentType, Integer parentId) {
        // TODO 지원 [1] ~ [8] 구현
        return null;
    }

    /**
     * 파일 단건 조회 (FileController 소유자 검증용)
     *
     * TODO 지원 [1]: fileMapper.findById(fileId) 호출
     * TODO 지원 [2]: null 이면 throw new CustomException(ErrorCode.FILE_NOT_FOUND)
     * TODO 지원 [3]: 조회된 FileDTO 반환
     */
    public FileDTO findById(int fileId) {
        // TODO 지원 [1] ~ [3] 구현
        return null;
    }

    /**
     * 파일 단건 삭제 (DB 삭제 + GCS 이벤트)
     * FileDeleteEvent → 트랜잭션 커밋 후 GcsService.handleFileDelete() 실행
     *
     * TODO 지원 [1]: fileMapper.findById(fileId) 호출
     *               - null 이면 throw new CustomException(ErrorCode.FILE_NOT_FOUND)
     * TODO 지원 [2]: eventPublisher.publishEvent(new FileDeleteEvent(dto.getUploadPath()))
     *               - 커밋 후 GCS 삭제 트리거
     * TODO 지원 [3]: fileMapper.delete(fileId) 호출 (DB 삭제)
     */
    @Transactional
    public void delete(int fileId) {
        // TODO 지원 [1] ~ [3] 구현
    }

    /**
     * parentId 기준 파일 목록 조회
     * 포트폴리오 상세 조회 시 결과물 파일 리스트 가져올 때 사용
     *
     * TODO 지원 [1]: fileMapper.findByParent(parentType, parentId) 호출 후 반환
     */
    public List<FileDTO> findByParent(FileParentType parentType, int parentId) {
        // TODO 지원 [1] 구현
        return null;
    }

    /**
     * parentId 기준 파일 전체 삭제 (DB + GCS 이벤트)
     * 포트폴리오 삭제 시 PortfolioService 에서 호출
     *
     * TODO 지원 [1]: findByParent(parentType, parentId) 로 파일 목록 조회
     * TODO 지원 [2]: 각 파일마다 eventPublisher.publishEvent(new FileDeleteEvent(dto.getUploadPath()))
     *               - 커밋 후 GCS 파일 전체 삭제 트리거
     * TODO 지원 [3]: fileMapper.deleteByParent(parentType, parentId) 호출 (DB 일괄 삭제)
     */
    @Transactional
    public void deleteByParent(FileParentType parentType, int parentId) {
        // TODO 지원 [1] ~ [3] 구현
    }
}