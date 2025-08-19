package org.team.mealkitshop.repository.item;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import org.team.mealkitshop.common.*;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.Review;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ReviewRepositoryTests {

    @Autowired ReviewRepository reviewRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired MemberRepository memberRepository;

    Long itemId;
    Long anotherItemId;
    Long mno;

    @BeforeEach
    void setUp() {
        Member m = new Member();
        m.setEmail("review-repo@test.com");
        m.setPassword("pw");
        m.setMemberName("리뷰리포");
        m.setPhone("010-3333-4444");
        mno = memberRepository.save(m).getMno();

        Item A = Item.builder().itemNm("볶음밥A").price(7000).stockNumber(100)
                .itemDetail("볶음밥").itemSellStatus(ItemSellStatus.SELL).build();
        A.setFoodItem(FoodItem.FRIED_RICE);
        itemId = itemRepository.save(A).getId();

        Item B = Item.builder().itemNm("치킨B").price(12000).stockNumber(50)
                .itemDetail("치킨").itemSellStatus(ItemSellStatus.SELL).build();
        B.setFoodItem(FoodItem.CHICKEN_BREAST);
        anotherItemId = itemRepository.save(B).getId();

        reviewRepository.save(make(m, A, "최고", 5));
        reviewRepository.save(make(m, A, "좋음", 4));
        reviewRepository.save(make(m, B, "그럭저럭", 3));
    }

    private Review make(Member m, Item item, String content, int rating) {
        Review r = new Review();
        r.setMember(m); r.setItem(item); r.setContent(content); r.setRating(rating);
        return r;
    }

    @Test @DisplayName("아이템 기준 페이지 조회/평균/카운트")
    void list_avg_count() {
        var page = reviewRepository.findByItem_Id(itemId, PageRequest.of(0, 10));

        // 작성자 이름 검증 추가
        assertThat(page.getContent())
                .extracting(r -> r.getMember().getMemberName())
                .containsOnly("리뷰리포");

        assertThat(page.getTotalElements()).isEqualTo(2);
        Double avg = reviewRepository.getAverageRatingByItemId(itemId);
        assertThat(avg).isEqualTo(4.5);

        long cnt = reviewRepository.countByItem_Id(itemId);
        assertThat(cnt).isEqualTo(2);
    }

    @Test @DisplayName("회원 기준 페이지 조회 및 중복 여부")
    void byMember_exists() {
        var page = reviewRepository.findByMember_Mno(mno, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(3);

        boolean exists = reviewRepository.existsByMember_MnoAndItem_Id(mno, itemId);
        assertThat(exists).isTrue();
    }

    @Test @DisplayName("Top10: 평점 내림차순, 동점 id 내림차순")
    void top10() {
        var list = reviewRepository.findTop10ByItem_IdOrderByRatingDescIdDesc(itemId);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getRating()).isGreaterThanOrEqualTo(list.get(1).getRating());
    }

    @Test @DisplayName("집계: 평균/개수 (IN 절)")
    void aggregations_in() {
        List<Long> ids = List.of(itemId, anotherItemId);

        List<Map<String, Object>> avgs = reviewRepository.findAvgRatingByItemIds(ids);
        List<Map<String, Object>> cnts = reviewRepository.findReviewCountByItemIds(ids);

        var Aavg = avgs.stream().filter(m -> ((Number)m.get("itemId")).longValue()==itemId).findFirst().orElseThrow();
        var Acnt = cnts.stream().filter(m -> ((Number)m.get("itemId")).longValue()==itemId).findFirst().orElseThrow();
        assertThat(((Number)Aavg.get("avgRating")).doubleValue()).isEqualTo(4.5);
        assertThat(((Number)Acnt.get("reviewCount")).longValue()).isEqualTo(2L);

        var Bavg = avgs.stream().filter(m -> ((Number)m.get("itemId")).longValue()==anotherItemId).findFirst().orElseThrow();
        var Bcnt = cnts.stream().filter(m -> ((Number)m.get("itemId")).longValue()==anotherItemId).findFirst().orElseThrow();
        assertThat(((Number)Bavg.get("avgRating")).doubleValue()).isEqualTo(3.0);
        assertThat(((Number)Bcnt.get("reviewCount")).longValue()).isEqualTo(1L);
    }

    @Test @DisplayName("벌크 삭제: 아이템 기준 리뷰 삭제")
    void bulk_deleteByItem() {
        assertThat(reviewRepository.countByItem_Id(itemId)).isEqualTo(2);
        reviewRepository.deleteByItemId(itemId);
        assertThat(reviewRepository.countByItem_Id(itemId)).isZero();
    }
}
