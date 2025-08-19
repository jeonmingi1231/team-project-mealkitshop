package org.team.mealkitshop.service.item;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.Review;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.item.ReviewDTO;
import org.team.mealkitshop.dto.item.ReviewImageDTO;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.item.ReviewRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.List;
import java.util.Map;

/**
 * ReviewService
 * --------------------------------------------------------------------
 * - 역할:
 *   • 리뷰(Review) 작성/수정/삭제/조회 관련 비즈니스 로직 처리
 *   • 리뷰 이미지 파일은 ReviewImageService와 협업
 *   • 아이템/회원 기준 조회, 평균 평점 및 리뷰 수 통계 기능 제공
 * - 의존성:
 *   • ReviewRepository : 리뷰 CRUD 및 조회
 *   • MemberRepository : 리뷰 작성자(Member) 참조
 *   • ItemRepository   : 리뷰 대상 Item 참조
 *   • ReviewImageService : 이미지 업로드/삭제/조회 처리
 * - 트랜잭션:
 *   • 클래스 레벨 @Transactional(rollbackFor = Exception.class)
 *   • 조회 메서드는 @Transactional(readOnly = true)
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final MemberRepository memberRepo;
    private final ItemRepository itemRepo;
    private final ReviewImageService reviewImageService;

    /* ==================== CREATE ==================== */

    /**
     * 리뷰 생성 (+ 선택적 이미지 업로드)
     * - 동일 회원이 동일 상품에 리뷰 중복 작성 불가
     * - Review 엔티티 저장 후, 이미지가 있으면 업로드
     *
     * @param dto 리뷰 DTO
     * @param imageFiles 업로드할 이미지 파일 리스트
     * @return 생성된 리뷰 ID
     */
    public Long create(ReviewDTO dto, List<MultipartFile> imageFiles) {
        if (reviewRepo.existsByMember_MnoAndItem_Id(dto.getWriterMno(), dto.getItemId())) {
            throw new IllegalStateException("이미 이 상품에 리뷰를 작성했습니다.");
        }

        Member memberRef = memberRepo.getReferenceById(dto.getWriterMno());
        Item itemRef = itemRepo.getReferenceById(dto.getItemId());

        Review r = new Review();
        r.setMember(memberRef);
        r.setItem(itemRef);
        r.setContent(dto.getContent());
        r.setRating(dto.getRating());

        Review saved = reviewRepo.save(r);

        if (imageFiles != null && !imageFiles.isEmpty()) {
            reviewImageService.addImages(saved.getId(), imageFiles);
        }
        return saved.getId();
    }

    /* ==================== UPDATE ==================== */

    /**
     * 리뷰 수정
     * - 본문(content) / 평점(rating) 수정
     * - replaceImages=true  → 기존 이미지 삭제 후 새 이미지 교체
     * - replaceImages=false → 기존 유지 + 새 이미지 있으면 추가
     *
     * @param dto 수정할 리뷰 DTO
     * @param newImageFiles 새 이미지 파일 리스트
     * @param replaceImages 기존 이미지 교체 여부
     */
    public void update(ReviewDTO dto, List<MultipartFile> newImageFiles, boolean replaceImages) {
        Review review = reviewRepo.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다. id=" + dto.getId()));

        review.setContent(dto.getContent());
        review.setRating(dto.getRating());

        if (replaceImages) {
            reviewImageService.replaceImages(review.getId(), newImageFiles);
        } else if (newImageFiles != null && !newImageFiles.isEmpty()) {
            reviewImageService.addImages(review.getId(), newImageFiles);
        }
    }

    /* ==================== DELETE ==================== */

    /**
     * 리뷰 삭제
     * - 관련 이미지 파일도 함께 삭제
     *
     * @param reviewId 리뷰 ID
     */
    public void delete(Long reviewId) {
        reviewImageService.deleteByReview(reviewId);
        reviewRepo.deleteById(reviewId);
    }

    /* ==================== READ ==================== */

    /**
     * 특정 아이템 기준 리뷰 목록 (페이징)
     *
     * @param itemId 아이템 ID
     * @param pageable 페이징 조건
     * @param withImages 이미지 포함 여부
     */
    @Transactional(readOnly = true)
    public Page<ReviewDTO> listByItem(Long itemId, Pageable pageable, boolean withImages) {
        Page<Review> page = reviewRepo.findByItem_Id(itemId, pageable);
        return mapReviewPage(page, withImages);
    }

    /**
     * 특정 회원 기준 리뷰 목록 (페이징)
     *
     * @param mno 회원 PK
     * @param pageable 페이징 조건
     * @param withImages 이미지 포함 여부
     */
    @Transactional(readOnly = true)
    public Page<ReviewDTO> listByMember(Long mno, Pageable pageable, boolean withImages) {
        Page<Review> page = reviewRepo.findByMember_Mno(mno, pageable);
        return mapReviewPage(page, withImages);
    }

    /**
     * 특정 아이템의 상위 10개 리뷰
     * - 정렬: 평점 desc, 동점 시 id desc
     *
     * @param itemId 아이템 ID
     * @param withImages 이미지 포함 여부
     */
    @Transactional(readOnly = true)
    public List<ReviewDTO> top10ByItem(Long itemId, boolean withImages) {
        List<Review> list = reviewRepo.findTop10ByItem_IdOrderByRatingDescIdDesc(itemId);
        return mapReviews(list, withImages);
    }

    /**
     * 특정 아이템의 평균 평점
     *
     * @param itemId 아이템 ID
     * @return 평균 평점(없으면 0.0)
     */
    @Transactional(readOnly = true)
    public double averageRating(Long itemId) {
        Double avg = reviewRepo.getAverageRatingByItemId(itemId);
        return avg == null ? 0.0 : avg;
    }

    /**
     * 특정 아이템의 리뷰 개수
     *
     * @param itemId 아이템 ID
     * @return 리뷰 개수
     */
    @Transactional(readOnly = true)
    public long reviewCount(Long itemId) {
        return reviewRepo.countByItem_Id(itemId);
    }

    /**
     * 아이템 제거 시 리뷰 일괄 정리(이미지 파일 포함)
     *
     * @param itemId 아이템 ID
     */
    public void deleteByItemId(Long itemId) {
        Page<Review> page = reviewRepo.findByItem_Id(itemId, Pageable.unpaged());
        page.getContent().forEach(r -> reviewImageService.deleteByReview(r.getId()));
        reviewRepo.deleteByItemId(itemId);
    }

    /* ==================== 공통 매핑 헬퍼(중복 제거) ==================== */

    /**
     * 리뷰 페이지 → DTO 페이지 변환 (withImages 옵션 처리)
     */
    @Transactional(readOnly = true)
    protected Page<ReviewDTO> mapReviewPage(Page<Review> page, boolean withImages) {
        if (!withImages) {
            return page.map(this::toDTOWithoutImages);
        }
        List<Long> ids = page.getContent().stream().map(Review::getId).toList();
        Map<Long, List<ReviewImageDTO>> grouped = reviewImageService.listByReviewIdsGrouped(ids);
        return page.map(r -> toDTOWithImages(r, grouped.getOrDefault(r.getId(), List.of())));
    }

    /**
     * 리뷰 목록 → DTO 목록 변환 (withImages 옵션 처리)
     */
    @Transactional(readOnly = true)
    protected List<ReviewDTO> mapReviews(List<Review> reviews, boolean withImages) {
        if (!withImages) {
            return reviews.stream().map(this::toDTOWithoutImages).toList();
        }
        List<Long> ids = reviews.stream().map(Review::getId).toList();
        Map<Long, List<ReviewImageDTO>> grouped = reviewImageService.listByReviewIdsGrouped(ids);
        return reviews.stream()
                .map(r -> toDTOWithImages(r, grouped.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    /* ==================== 매핑 유틸 ==================== */

    /** Review → ReviewDTO 변환 (이미지 제외) */
    private ReviewDTO toDTOWithoutImages(Review r) {
        return ReviewDTO.builder()
                .id(r.getId())
                .itemId(r.getItem().getId())
                .writerMno(r.getMember().getMno())
                .writerName(safeMemberName(r))
                .content(r.getContent())
                .rating(r.getRating())
                .regTime(r.getRegTime())
                .updateTime(r.getUpdateTime())
                .images(List.of())
                .build();
    }

    /** Review → ReviewDTO 변환 (이미지 포함) */
    private ReviewDTO toDTOWithImages(Review r, List<ReviewImageDTO> images) {
        return ReviewDTO.builder()
                .id(r.getId())
                .itemId(r.getItem().getId())
                .writerMno(r.getMember().getMno())
                .writerName(safeMemberName(r))
                .content(r.getContent())
                .rating(r.getRating())
                .regTime(r.getRegTime())
                .updateTime(r.getUpdateTime())
                .images(images == null ? List.of() : images)
                .build();
    }

    /**
     * 안전하게 작성자 이름 가져오기
     * - Member 엔티티가 lazy 로딩되거나 null일 수 있으므로 예외 처리
     */
    private String safeMemberName(Review r) {
        try {
            return r.getMember().getMemberName(); // Member 엔티티 실제 필드명에 맞게 조정
        } catch (Exception e) {
            return null;
        }
    }
}
