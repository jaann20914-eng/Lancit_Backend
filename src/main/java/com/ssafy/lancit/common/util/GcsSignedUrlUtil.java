package com.ssafy.lancit.common.util;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * GCS Signed URL 생성 유틸
 *
 * Signed URL 이란?
 *  - GCS 비공개 버킷의 파일을 특정 시간 동안만 접근 가능한 URL로 제공
 *  - 만료 후 403 Forbidden 반환
 *
 * 용도별 만료시간:
 *  - 프로필 이미지 / 포트폴리오 배너 : 7일 (자주 바뀌지 않음)
 *  - 계약서 PDF / 결과물 파일       : 15분 (다운로드 요청 시 그때그때 발급)
 *  - 서명 이미지                    : 1시간
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GcsSignedUrlUtil {

    private final Storage storage;

    @Qualifier("gcsBucketName")
    private final String bucketName;


     // 기본 Signed URL 생성 (단위: 분)
     // @param objectPath GCS 오브젝트 경로 (upload_path 컬럼값)
     // @param minutes    만료 시간 (분)
     // @return Signed URL 문자열
     // 이미지 화면에 뿌리는 용
    public String generateSignedUrl(String objectPath, long minutes) {
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectPath).build();

        String url = storage.signUrl(
                blobInfo,
                minutes,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature()
        ).toString();

        log.debug("[GcsSignedUrl] 발급 완료 - path: {}, 만료: {}분", objectPath, minutes);
        return url;
    }
    
    //포트폴리오 등 파일다운로드용
    public String getDownloadUrl(String objectPath, String oriName) {
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectPath)
                .setContentDisposition("attachment; filename=\"" + oriName + "\"")
                .build();
        return storage.signUrl(
                blobInfo,
                60,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature()
        ).toString();
    }
    

     // 프로필 이미지 / 포트폴리오 배너용 (7일)
     // Redis 에 6일 TTL 로 캐싱해서 재사용
    public String generateForImage(String objectPath) {
        return generateSignedUrl(objectPath, TimeUnit.DAYS.toMinutes(7));
    }
    
    //계약서 PDF / 결과물 파일 다운로드용 (15분)
    //다운로드 요청 시마다 새로 발급 (캐싱 X)
    public String getDownloadContractUrl(String objectPath) {
        return generateSignedUrl(objectPath, 15);
    }

    //서명 이미지용 (1시간)
    public String generateForSign(String objectPath) {
        return generateSignedUrl(objectPath, 60);
    }
}