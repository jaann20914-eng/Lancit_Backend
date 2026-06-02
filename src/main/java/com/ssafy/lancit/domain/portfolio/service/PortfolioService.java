package com.ssafy.lancit.domain.portfolio.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.annotation.OwnerCheck;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;

import lombok.RequiredArgsConstructor;

// 포트폴리오 CRUD - 프리랜서 전용
// 배너/결과물 파일은 FileService 에서 처리 (GCS + Redis Signed URL 캐시)
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioMapper portfolioMapper;
    private final FileService fileService;

    // PORT-01 내 포트폴리오 목록 조회 (페이지네이션)
    public PageResponse<PortfolioDTO> getMyList(String email, PageRequest pageRequest) {
        // TODO 영은 [1]: List<PortfolioDTO> list = portfolioMapper.findByEmail(email, pageRequest)
        // TODO 영은 [2]: long total = portfolioMapper.countByEmail(email)
        // TODO 영은 [3]: return PageResponse.of(list, total, pageRequest)
        List<PortfolioDTO> list = portfolioMapper.findByEmail(email, pageRequest);
        long total = portfolioMapper.countByEmail(email);
        return PageResponse.of(list, total, pageRequest);
    }

    // CLI-SEAR-02 회사가 특정 프리랜서 공개 포트폴리오 조회 (페이지네이션)
    public PageResponse<PortfolioDTO> getPublicList(String email, PageRequest pageRequest) {
        // TODO 영은 [1]: List<PortfolioDTO> list = portfolioMapper.findPublicByEmail(email, pageRequest)
        // TODO 영은 [2]: long total = portfolioMapper.countPublicByEmail(email)
        // TODO 영은 [3]: return PageResponse.of(list, total, pageRequest)
        List<PortfolioDTO> list = portfolioMapper.findPublicByEmail(email, pageRequest);
        long total = portfolioMapper.countPublicByEmail(email);
        return PageResponse.of(list, total, pageRequest);
    }

 // PORT-02 포트폴리오 상세 조회 - Map 으로 조립해서 반환
    public Map<String, Object> getOne(int portfolioId) {
        // TODO 영은 [1]: PortfolioDTO dto = portfolioMapper.findById(portfolioId)
        // TODO 영은 [2]: null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
        // TODO 영은 [3]: List<FileDTO> files = fileService.findByParent(FileParentType.PORTFOLIO, portfolioId)
        // TODO 영은 [4]: Map<String, Object> result = new HashMap<>()
        //               result.put("portfolio", dto)
        //               result.put("files", files)
        //               return result
        return null;
    }

    // PORT-03 포트폴리오 등록
    // 배너 이미지는 컨트롤러 호출 전 POST /api/files/upload 로 먼저 업로드
    @Transactional
    public void create(PortfolioDTO dto, String email) {
        // TODO 영은 [1]: dto.setEmail(email)
        // TODO 영은 [2]: portfolioMapper.insert(dto)
    }

    // PORT-03 포트폴리오 수정 (@OwnerCheck 로 소유자 검증)
    @OwnerCheck(resourceType = "PORTFOLIO")
    @Transactional
    public void update(int portfolioId, PortfolioDTO dto) {
        // TODO 영은 [1]: portfolioMapper.update(portfolioId, dto)
    }

    // PORT-04 포트폴리오 삭제 (@OwnerCheck 로 소유자 검증)
    // 정석: GCS + Redis 먼저 정리 → DB CASCADE 로 마무리
    @OwnerCheck(resourceType = "PORTFOLIO")
    @Transactional
    public void delete(int portfolioId) {
        // TODO 영은 [1]: ★ GCS + Redis 정리 (CASCADE 전에 먼저 처리)
        //               fileService.deleteByParent(FileParentType.PORTFOLIO, portfolioId)
        //               → 배너 + 결과물 파일 전부 GCS 삭제 + Redis @CacheEvict
        // TODO 영은 [2]: portfolioMapper.delete(portfolioId)
        //               → CASCADE: portfolio_permission 자동 삭제
    }
}