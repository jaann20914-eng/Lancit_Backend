package com.ssafy.lancit.domain.file.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.lancit.common.annotation.OwnerCheck;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.GcsSignedUrlUtil;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.event.FileDeleteEvent;
import com.ssafy.lancit.domain.file.mapper.FileDeleteQueueMapper;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationPortfolioSnapshotMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationProfileSnapshotMapper;
import com.ssafy.lancit.global.enums.FileParentType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 파일 업로드 / 조회 / 삭제
// ★ Redis: @Cacheable(signedUrl, 6일) / @CacheEvict(삭제 시 자동 제거)
// ★ GCS: 업로드/삭제 처리, 삭제는 FileDeleteEvent → 트랜잭션 커밋 후 실행
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileMapper fileMapper;
    private final GcsService gcsService;
    private final GcsSignedUrlUtil gcsSignedUrlUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final FileDeleteQueueMapper fileDeleteQueueMapper;
    private final PortfolioMapper portfolioMapper;
    private final ApplicationPortfolioSnapshotMapper applicationPortfolioSnapshotMapper;
    private final ApplicationProfileSnapshotMapper applicationProfileSnapshotMapper;
    
    private final CacheManager cacheManager;
    
    // 파일 업로드 - GCS 먼저 업로드 후 DB 저장
    // GCS 성공 + DB 실패 시 GCS 수동 롤백 처리
    @Transactional
    public List<FileDTO> upload(List<MultipartFile> files, FileParentType parentType,
                          Integer parentId, String email, String role) {

        validatePortfolioUpload(parentType, parentId, email, role);
    	
    	// TEMP 업로드 시 기존 TEMP 파일 삭제 큐에 추가
        // (여러 번 바꿨을 때 이전 TEMP 파일 정리)
        if (FileParentType.TEMP.equals(parentType)) {
            List<FileDTO> existingTemps = fileMapper.findTempByEmail(email);
            for (FileDTO old : existingTemps) {
                fileDeleteQueueMapper.insert(old.getUploadPath());
                fileMapper.delete(old.getFileId());
                cacheManager.getCache("signedUrl").evict(old.getFileId());
            }
        }
    	
     //TEMP_SIGNATURE도 동일하게 정리
//        if (FileParentType.TEMP_SIGNATURE.equals(parentType)) {
//            List<FileDTO> existingTempSigs = fileMapper.findTempSignatureByEmail(email);
//            for (FileDTO old : existingTempSigs) {
//                fileDeleteQueueMapper.insert(old.getUploadPath());
//                fileMapper.delete(old.getFileId());
//                cacheManager.getCache("signedUrl").evict(old.getFileId());
//            }
//        }
    	
    	List<FileDTO> result = new ArrayList<>();
    	
    	for(MultipartFile file : files) {
    		String sysName=null;
			try {
				sysName = gcsService.upload(file, parentType);
				FileDTO dto = FileDTO.builder()
						.userEmail("user".equalsIgnoreCase(role) ? email : null) // 유저면 유저로 
						.companyEmail("user".equalsIgnoreCase(role) ? null : email)// 회사면 회사로
	    		        .sysName(sysName)
	    		        .oriName(file.getOriginalFilename())
	    		        .parentType(parentType)
	    		        .parentId(parentId)
	    		        .uploadPath(sysName)
	    		        .fileSize((int) file.getSize())
	    		        .build();
				fileMapper.insert(dto);
				result.add(dto);
				
				
			} catch (Exception e) {
			    e.printStackTrace(); // TODO 지원 : 개발 완료 후 삭제
			    if (sysName != null) {
			        result.forEach(uploaded -> gcsService.deleteByPath(uploaded.getSysName()));
			        gcsService.deleteByPath(sysName); // 현재 실패한 파일 롤백
			    }
			    throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
			}
    	}
        return result;
    }

    private void validatePortfolioUpload(FileParentType parentType, Integer parentId,
                                         String email, String role) {
        if (!FileParentType.PORTFOLIO_FILE.equals(parentType)
                && !FileParentType.PORTFOLIO_BANNER.equals(parentType)) {
            return;
        }
        if (!"USER".equalsIgnoreCase(role)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (parentId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        PortfolioDTO portfolio = portfolioMapper.findById(parentId);
        if (portfolio == null) {
            throw new CustomException(ErrorCode.PORTFOLIO_NOT_FOUND);
        }
        if (!email.equals(portfolio.getEmail())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    
    //계약서 파일 업로드 (계약서만 사용 다른건 사용xxxxxx)
    @Transactional
    public FileDTO uploadContractPdf(
            MultipartFile file,
            Integer contractId,
            String companyEmail
    ) {

        List<FileDTO> result = upload(
                List.of(file),
                FileParentType.CONTRACT,
                contractId,
                companyEmail,
                "COMPANY"
        );

        return result.get(0);
    }
    
    
    
    // 파일 단건 조회
    public FileDTO findById(int fileId) {
        FileDTO dto = fileMapper.findById(fileId);
        if (dto == null) throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        return dto;
    }

    
    
    // Signed URL 조회
    // Redis "signedUrl:{fileId}" 캐싱 TTL 6일
    // 최초 조회 시 GCS 발급 후 Redis 저장, 이후 Redis 에서 즉시 반환
    @Cacheable(value = "signedUrl", key = "#fileId")
    public String getSignedUrl(int fileId) {
        FileDTO dto = findById(fileId);
        return gcsSignedUrlUtil.generateForImage(dto.getUploadPath());
    }

    public void validateReadAccess(int fileId, String viewerEmail) {
        FileDTO file = findById(fileId);
        if (FileParentType.PROFILE.equals(file.getParentType())
                || FileParentType.PORTFOLIO_PROFILE.equals(file.getParentType())) {
            requireFileOwner(file, viewerEmail);
            return;
        }
        if (!FileParentType.PORTFOLIO_BANNER.equals(file.getParentType())
                && !FileParentType.PORTFOLIO_FILE.equals(file.getParentType())) {
            return;
        }

        if (file.getParentId() == null) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        PortfolioDTO portfolio = portfolioMapper.findById(file.getParentId());
        if (portfolio == null) {
            throw new CustomException(ErrorCode.PORTFOLIO_NOT_FOUND);
        }
        if (!viewerEmail.equals(portfolio.getEmail()) && !Boolean.TRUE.equals(portfolio.getIsPublic())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireFileOwner(FileDTO file, String viewerEmail) {
        String ownerEmail = file.getUserEmail() != null ? file.getUserEmail() : file.getCompanyEmail();
        if (!viewerEmail.equals(ownerEmail)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    
    // 다운로드 링크 
    // FileService 에 추가
    public String getDownloadUrl(int fileId) {
        FileDTO dto = findById(fileId);
        // Redis 캐싱 안 함 (짧은 TTL 이라 의미없음)
        return gcsSignedUrlUtil.getDownloadUrl(dto.getUploadPath(), dto.getOriName());
    }
    
    
    
    // 파일 단건 삭제 : 파일 소유자만삭제 가능
    @OwnerCheck(resourceType = "FILE") // 1.오너 체크먼저 AOP로 진행 : (현재 로그인한 이메일 + 파일 소유자 조회 + 같으면 통과 + 다르면 예외)
    @Transactional // file_db--portfolio_db 등 트랜잭션 처리
    public void delete(int fileId) {
        
        FileDTO dto = fileMapper.findById(fileId);
        if(dto == null) return;
        deleteOrDetachSnapshotFile(dto);
        // 트랜잭션이 정상적으로 COMMIT 된 후 실행됨
        // 단, gcs까지는 삭제를 보장하지 않고 트랜잭션 처리가 안됨. 삭제 실패한 파일 목록들 저장해서 배치로 재시도
    }
    
    
	 // 시스템(도메인 서비스) 주도 삭제 - OwnerCheck 미적용 : 개인것만 지워야하는 상화에서는 사용 금지
	 // 계약 파기/완료 등 계약 도메인이 자신의 PDF/첨부파일을 정리할 때 사용
	 @Transactional
    public void deleteBySystem(int fileId) {
	
	     FileDTO dto = fileMapper.findById(fileId);
	     if (dto == null) return;
	
         deleteOrDetachSnapshotFile(dto);
	 }
    


    // parentId 기준 파일 목록 조회
    public List<FileDTO> findByParent(FileParentType parentType, int parentId) {
        return fileMapper.findByParent(parentType, parentId);
    }

    
    
    // parentId 기준 파일 목록 전부 삭제
    // 캐시/GCS 처리 완료
    @Transactional
    public void deleteByParent(FileParentType parentType, int parentId) {
        List<FileDTO> files = findByParent(parentType, parentId);
        if (files == null || files.isEmpty()) return;
        
        for (FileDTO dto : files) {
            deleteOrDetachSnapshotFile(dto);
        }
    }

    @Transactional
    public void deleteProfileIfUnreferenced(int fileId) {
        if (fileMapper.isCurrentProfileFileReferenced(fileId)) {
            return;
        }
        deleteBySystem(fileId);
    }

    @Transactional
    public void deletePortfolioFileIfUnreferenced(int fileId) {
        if (fileMapper.isCurrentPortfolioFileReferenced(fileId)
                || applicationPortfolioSnapshotMapper.isFileReferenced(fileId)) {
            return;
        }
        deleteBySystem(fileId);
    }

    @Transactional
    public void deleteRecruitmentImageIfUnreferenced(int fileId) {
        if (fileMapper.isCurrentRecruitmentImageReferenced(fileId)) {
            return;
        }
        FileDTO dto = fileMapper.findById(fileId);
        if (dto == null) {
            return;
        }
        deleteFile(dto);
    }

    private void deleteOrDetachSnapshotFile(FileDTO dto) {
        boolean snapshotReferenced = applicationProfileSnapshotMapper.isProfileFileReferenced(dto.getFileId())
                || applicationPortfolioSnapshotMapper.isFileReferenced(dto.getFileId());
        if (snapshotReferenced) {
            fileMapper.detach(dto.getFileId());
            return;
        }

        deleteFile(dto);
    }

    private void deleteFile(FileDTO dto) {
        eventPublisher.publishEvent(new FileDeleteEvent(dto.getUploadPath()));
        fileMapper.delete(dto.getFileId());
        if (cacheManager.getCache("signedUrl") != null) {
            cacheManager.getCache("signedUrl").evict(dto.getFileId());
        }
    }
    
    @Transactional
    public void promote(Integer fileId, FileParentType targetType) {

        FileDTO file = fileMapper.findById(fileId);

        if (file == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        String newPath = gcsService.move(
                file.getSysName(),
                targetType);

        fileMapper.updatePath(fileId, newPath);
        fileMapper.updateParentType(fileId, targetType);
        
        // ✅ 추가: 경로가 바뀌었으므로 캐시 무효화
        if (cacheManager.getCache("signedUrl") != null) {
            cacheManager.getCache("signedUrl").evict(fileId);
        }
    }

    @Transactional
    public void promoteOwned(Integer fileId, FileParentType targetType, String ownerEmail) {
        FileDTO file = findById(fileId);
        String fileOwnerEmail = file.getUserEmail() != null ? file.getUserEmail() : file.getCompanyEmail();
        if (!ownerEmail.equals(fileOwnerEmail)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (targetType.equals(file.getParentType())) {
            return;
        }
//        if (!FileParentType.TEMP.equals(file.getParentType())) {
//            throw new CustomException(ErrorCode.INVALID_INPUT);
//        }
        if (!FileParentType.TEMP.equals(file.getParentType())
        	    && !FileParentType.TEMP_SIGNATURE.equals(file.getParentType())) {
        	    throw new CustomException(ErrorCode.INVALID_INPUT);
        	}

        String newPath = gcsService.move(file.getSysName(), targetType);
        fileMapper.updatePath(fileId, newPath);
        fileMapper.updateParentType(fileId, targetType);
        
        if (cacheManager.getCache("signedUrl") != null) {
            cacheManager.getCache("signedUrl").evict(fileId);
        }
    }
    
    
    
    @Transactional
    public void attachToParent(Integer fileId, FileParentType targetType, int parentId, String ownerEmail) {
        if (fileId == null) { return; }

        FileDTO file = findById(fileId);

        String fileOwnerEmail = file.getUserEmail() != null ? file.getUserEmail() : file.getCompanyEmail();
        if (!ownerEmail.equals(fileOwnerEmail)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (file.getParentId() != null && !file.getParentId().equals(parentId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // TEMP 또는 TEMP_SIGNATURE 에서만 승격 가능
        // 이미 targetType 이면 재호출 허용
        if (!FileParentType.TEMP.equals(file.getParentType())
                && !FileParentType.TEMP_SIGNATURE.equals(file.getParentType())
                && !targetType.equals(file.getParentType())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // TEMP 계열이면 GCS 경로도 이동
        if (FileParentType.TEMP.equals(file.getParentType())
                || FileParentType.TEMP_SIGNATURE.equals(file.getParentType())) {
            String newPath = gcsService.move(file.getSysName(), targetType);
            try {
                fileMapper.updatePath(fileId, newPath);
                fileMapper.updateParent(fileId, targetType, parentId);
            } catch (RuntimeException e) {
                restoreTempPath(newPath);
                throw e;
            }
            registerMoveRollback(newPath);
            // ✅ 추가
            if (cacheManager.getCache("signedUrl") != null) {
                cacheManager.getCache("signedUrl").evict(fileId);
            }
            
            return;
        }

        fileMapper.updateParent(fileId, targetType, parentId);
        
     
    }

    
    

    private void registerMoveRollback(String promotedPath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    restoreTempPath(promotedPath);
                }
            }
        });
    }

    private void restoreTempPath(String promotedPath) {
        try {
            gcsService.move(promotedPath, FileParentType.TEMP);
        } catch (RuntimeException rollbackException) {
            log.error("[GCS] TEMP 파일 승격 롤백 실패: {}", promotedPath, rollbackException);
        }
    }

    @Transactional
    public void detachByParent(FileParentType parentType, int parentId) {
        fileMapper.detachByParent(parentType, parentId);
    }

    @Transactional
    public void detach(int fileId) {
        fileMapper.detach(fileId);
    }
    
    
}
