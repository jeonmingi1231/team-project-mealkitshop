package org.team.mealkitshop.service.item;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.Review;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.item.ReviewDTO;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.item.ReviewRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ReviewServiceTest {

    @Autowired ReviewService reviewService;

    @MockitoBean ReviewRepository reviewRepository;
    @MockitoBean ItemRepository itemRepository;
    @MockitoBean MemberRepository memberRepository;
    @MockitoBean ReviewImageService reviewImageService;

    private Item item(Long id) { Item i = new Item(); i.setId(id); return i; }
    private Member member(Long mno, String email, String name) {
        Member m = new Member();
        m.setMno(mno);
        m.setEmail(email);
        m.setMemberName(name);
        return m;
    }
    private Review review(Long id, Item it, Member mem) {
        Review r = new Review();
        r.setId(id);
        r.setItem(it);
        r.setMember(mem);
        r.setRating(5);
        r.setContent("아주 맛있어요");
        return r;
    }

    @Test
    @DisplayName("리뷰 생성 - 중복 방지/이미지 추가 포함")
    void create_success() {
        ReviewDTO dto = new ReviewDTO();
        dto.setWriterMno(1L);
        dto.setItemId(10L);
        dto.setContent("맛있어요");
        dto.setRating(5);

        given(reviewRepository.existsByMember_MnoAndItem_Id(1L, 10L)).willReturn(false);
        given(memberRepository.getReferenceById(1L)).willReturn(member(1L, "user@test.com", "홍길동"));
        given(itemRepository.getReferenceById(10L)).willReturn(item(10L));
        given(reviewRepository.save(any(Review.class))).willAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        List<MultipartFile> images = List.of(); // 빈 리스트로 호출(이미지 없는 경우)
        Long id = reviewService.create(dto, images);

        assertThat(id).isEqualTo(100L);
        then(reviewRepository).should().save(any(Review.class));
        // 이미지가 있었다면 reviewImageService.addImages(100L, images) 호출됨
    }

    @Test
    @DisplayName("리뷰 생성 - 동일 회원/상품 중복 작성 시 IllegalStateException")
    void create_duplicate() {
        ReviewDTO dto = new ReviewDTO();
        dto.setWriterMno(1L);
        dto.setItemId(10L);
        dto.setContent("x"); dto.setRating(3);

        given(reviewRepository.existsByMember_MnoAndItem_Id(1L, 10L)).willReturn(true);

        assertThatThrownBy(() -> reviewService.create(dto, List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("리뷰 수정 - 본문/평점 변경, 이미지 교체/추가 분기")
    void update_success_replace_or_add_images() {
        Item it = item(10L);
        Member mem = member(1L, "author@test.com", "작성자");
        Review existing = review(77L, it, mem);

        given(reviewRepository.findById(77L)).willReturn(Optional.of(existing));
        given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));

        ReviewDTO patch = new ReviewDTO();
        patch.setId(77L);
        patch.setContent("수정 내용");
        patch.setRating(3);

        // replaceImages = true → 교체
        reviewService.update(patch, List.of(), true);
        then(reviewImageService).should().replaceImages(eq(77L), anyList());

        // replaceImages = false & 새 이미지 존재 → 추가
        reset(reviewImageService);
        MultipartFile mf = mock(MultipartFile.class);                          // ★ 목 파일 하나 만들어서
        reviewService.update(patch, List.of(mf), false);           // ★ 비어있지 않은 리스트로 호출
        then(reviewImageService).should().addImages(eq(77L), anyList());
    }

    @Test
    @DisplayName("리뷰 삭제 - 이미지 정리 후 삭제")
    void delete_success() {
        willDoNothing().given(reviewImageService).deleteByReview(100L);
        willDoNothing().given(reviewRepository).deleteById(100L);

        reviewService.delete(100L);

        then(reviewImageService).should().deleteByReview(100L);
        then(reviewRepository).should().deleteById(100L);
    }

    @Test
    @DisplayName("아이템 기준 리뷰 페이지 조회 - withImages 분기")
    void listByItem_paging_with_or_without_images() {
        Item it = item(10L);
        Member m = member(1L, "user@test.com", "홍길동");
        Review r1 = review(1L, it, m);
        Review r2 = review(2L, it, m);

        Pageable pageable = PageRequest.of(0, 2);
        Page<Review> page = new PageImpl<>(List.of(r1, r2), pageable, 2);
        given(reviewRepository.findByItem_Id(10L, pageable)).willReturn(page);

        // withImages=false
        Page<ReviewDTO> p1 = reviewService.listByItem(10L, pageable, false);
        assertThat(p1.getContent()).hasSize(2);

        // withImages=true -> 이미지 그룹 조회 호출
        reset(reviewImageService);
        given(reviewImageService.listByReviewIdsGrouped(anyList())).willReturn(java.util.Map.of());
        Page<ReviewDTO> p2 = reviewService.listByItem(10L, pageable, true);
        assertThat(p2.getContent()).hasSize(2);
        then(reviewImageService).should().listByReviewIdsGrouped(anyList());
    }

    @Test
    @DisplayName("평균 평점/리뷰 수")
    void average_and_count() {
        given(reviewRepository.getAverageRatingByItemId(10L)).willReturn(4.2);
        given(reviewRepository.countByItem_Id(10L)).willReturn(7L);

        assertThat(reviewService.averageRating(10L)).isEqualTo(4.2);
        assertThat(reviewService.reviewCount(10L)).isEqualTo(7L);
    }
}
