package com.ssafy.lancit.domain.file.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.lancit.common.annotation.OwnerCheck;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.GcsSignedUrlUtil;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.event.FileDeleteEvent;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.global.enums.FileParentType;

import lombok.RequiredArgsConstructor;

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

    
    @Autowired
    private CacheManager cacheManager;
    
    // 파일 업로드 - GCS 먼저 업로드 후 DB 저장
    // GCS 성공 + DB 실패 시 GCS 수동 롤백 처리
    @Transactional
    public List<FileDTO> upload(List<MultipartFile> files, FileParentType parentType,
                          Integer parentId, String email, String role) {
    	List<FileDTO> result = new ArrayList<>();
    	
    	for(MultipartFile file : files) {
    		String sysName=null;
			try {
				sysName = gcsService.upload(file, parentType);
				FileDTO dto = FileDTO.builder()
	    		        .userEmail("USER".equals(role) ? email : null) // 유저면 유저로 
	    		        .companyEmail("USER".equals(role) ? null : email) // 회사면 회사로
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


    
    // 파일 단건 삭제
    @OwnerCheck(resourceType = "FILE") // 1.오너 체크먼저 AOP로 진행 : (현재 로그인한 이메일 + 파일 소유자 조회 + 같으면 통과 + 다르면 예외)
    @Transactional // file_db--portfolio_db 등 트랜잭션 처리
    public void delete(int fileId) {
        
    	FileDTO dto = fileMapper.findById(fileId);
        if(dto == null) return;
        
        eventPublisher.publishEvent(  new FileDeleteEvent(dto.getUploadPath()) ); // 커밋 성공하면 이 파일 삭제 예약
        fileMapper.delete(fileId);
        cacheManager.getCache("signedUrl").evict(dto.getFileId());//Redis signedUrl 캐시 제거
        // 트랜잭션이 정상적으로 COMMIT 된 후 실행됨
        // 단, gcs까지는 삭제를 보장하지 않고 트랜잭션 처리가 안됨. 삭제 실패한 파일 목록들 저장해서 배치로 재시도
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
            eventPublisher.publishEvent(new FileDeleteEvent(dto.getUploadPath()));
            fileMapper.delete(dto.getFileId());
            cacheManager.getCache("signedUrl").evict(dto.getFileId());//Redis signedUrl 캐시 제거
        }
    }
    
    
    
}