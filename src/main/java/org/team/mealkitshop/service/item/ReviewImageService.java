package org.team.mealkitshop.service.item;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.domain.item.Review;
import org.team.mealkitshop.domain.item.ReviewImage;
import org.team.mealkitshop.dto.item.ReviewImageDTO;
import org.team.mealkitshop.repository.item.ReviewImageRepository;
import org.team.mealkitshop.repository.item.ReviewRepository;


import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReviewImageService
 * --------------------------------------------------------------------
 * - 역할:
 *   • 리뷰(Review)에 첨부된 이미지 파일의 업로드/조회/삭제를 담당하는 서비스 계층
 *   • 실제 파일 저장/삭제는 FileService, DB 저장은 ReviewImageRepository 사용
 *   • 리뷰 삭제 시 연관된 이미지 파일도 함께 정리
 * - 트랜잭션:
 *   • 클래스 레벨 @Transactional(rollbackFor = Exception.class)
 *     → 서비스 메서드 수행 중 예외 발생 시 롤백
 *   • 조회 메서드에는 @Transactional(readOnly = true) 적용
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ReviewImageService {

    @Value("${uploadPath}")
    private String imgBase; // 업로드된 파일 접근 URL prefix

    private final FileService fileService;               // 실제 파일 입출력 담당
    private final ReviewImageRepository reviewImageRepo; // 리뷰 이미지 DB 저장소
    private final ReviewRepository reviewRepo;           // 리뷰 참조용

    /* ==================== CREATE ==================== */

    /**
     * 리뷰에 이미지 추가
     * - MultipartFile 리스트를 받아 저장
     * - 저장 실패 시 이미 저장된 파일 삭제(rollback 보완)
     *
     * @param reviewId 리뷰 ID
     * @param files 업로드할 이미지 파일 리스트
     */
    public void addImages(Long reviewId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return;

        Review reviewRef = reviewRepo.getReferenceById(reviewId); // 지연 로딩 프록시 참조
        List<ReviewImage> batch = new ArrayList<>();
        List<String> savedThisBatch = new ArrayList<>();

        try {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;

                String ori = Objects.requireNonNull(f.getOriginalFilename(), "original filename is null");

                // 파일 저장
                String saved;
                try (InputStream in = f.getInputStream()) {
                    saved = fileService.uploadFile(ori, in);
                }
                savedThisBatch.add(saved);

                // ReviewImage 엔티티 생성
                ReviewImage e = new ReviewImage();
                e.setReview(reviewRef);
                e.setOriImgName(ori);
                e.setImgName(saved);
                e.setImgUrl(imgBase + saved);
                batch.add(e);
            }
            if (!batch.isEmpty()) reviewImageRepo.saveAll(batch);
        } catch (Exception e) {
            cleanupSaved(savedThisBatch); // 실패 시 저장된 파일 정리
            throw new RuntimeException(e);
        }
    }

    /**
     * 리뷰 이미지 교체
     * - 기존 이미지를 모두 삭제한 뒤 새 이미지로 교체
     *
     * @param reviewId 리뷰 ID
     * @param files 새로 업로드할 이미지 리스트
     */
    public void replaceImages(Long reviewId, List<MultipartFile> files) {
        deleteByReview(reviewId);
        addImages(reviewId, files);
    }

    /* ==================== READ ==================== */

    /**
     * 특정 리뷰의 이미지 목록 조회 (정렬 보장)
     *
     * @param reviewId 리뷰 ID
     * @return 리뷰 이미지 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<ReviewImageDTO> listByReview(Long reviewId) {
        return reviewImageRepo.findByReview_IdOrderByIdAsc(reviewId).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * 특정 아이템에 속한 모든 리뷰 이미지 조회
     *
     * @param itemId 아이템 ID
     * @return 리뷰 이미지 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<ReviewImageDTO> listByItem(Long itemId) {
        return reviewImageRepo.findByReview_Item_Id(itemId).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * 여러 리뷰 ID에 대한 이미지를 그룹핑하여 조회
     * - reviewId 기준으로 Map<Long, List<ReviewImageDTO>> 반환
     *
     * @param reviewIds 리뷰 ID 리스트
     * @return Map<리뷰ID, 이미지DTO목록>
     */
    @Transactional(readOnly = true)
    public Map<Long, List<ReviewImageDTO>> listByReviewIdsGrouped(List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) return Collections.emptyMap();
        return reviewIds.stream().collect(Collectors.toMap(id -> id, this::listByReview));
    }

    /* ==================== DELETE ==================== */

    /**
     * 개별 리뷰 이미지 삭제
     * - 파일 삭제 + DB 레코드 삭제
     *
     * @param imageId 리뷰 이미지 ID
     */
    public void deleteImage(Long imageId) {
        ReviewImage img = reviewImageRepo.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("ReviewImage not found: " + imageId));
        try { fileService.deleteBySavedName(img.getImgName()); } catch (Exception ignore) {}
        reviewImageRepo.delete(img);
    }

    /**
     * 특정 리뷰에 속한 모든 이미지 삭제
     * - 파일 삭제 + DB 레코드 삭제
     *
     * @param reviewId 리뷰 ID
     */
    public void deleteByReview(Long reviewId) {
        reviewImageRepo.findByReview_Id(reviewId).forEach(img -> {
            try { fileService.deleteBySavedName(img.getImgName()); } catch (Exception ignore) {}
        });
        reviewImageRepo.deleteByReview_Id(reviewId);
    }

    /* ==================== HELPER ==================== */

    /** 업로드 실패 시 저장된 파일들을 정리(삭제) */
    private void cleanupSaved(List<String> savedNames) {
        for (String fn : savedNames) {
            try { fileService.deleteBySavedName(fn); } catch (Exception ignore) {}
        }
    }

    /** ReviewImage 엔티티 → DTO 변환 */
    private ReviewImageDTO toDTO(ReviewImage e) {
        return ReviewImageDTO.builder()
                .id(e.getId())
                .imgName(e.getImgName())
                .oriImgName(e.getOriImgName())
                .imgUrl(e.getImgUrl())
                .build();
    }
}
