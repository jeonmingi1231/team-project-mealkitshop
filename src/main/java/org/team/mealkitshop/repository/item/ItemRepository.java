package org.team.mealkitshop.repository.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;
import org.team.mealkitshop.common.Category;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.domain.item.Item;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>,
        QuerydslPredicateExecutor<Item>, ItemRepositoryCustom {

    // 기본 CRUD(JpaRepository), 동적 쿼리(Querydsl), 커스텀 기능 제공

    // 이름 완전일치 검색
    List<Item> findByItemNm(String itemNm);

    // 가격 미만 조회 + 페이징/정렬
    Page<Item> findByPriceLessThan(Integer price, Pageable pageable);

    // 이름/설명 부분검색 (대소문자 무시)
    Page<Item> findByItemNmContainingIgnoreCaseOrItemDetailContainingIgnoreCase(
            String name, String detail, Pageable pageable);

    // 카테고리, 판매상태 단독/복합 검색
    Page<Item> findByCategory(Category category, Pageable pageable);
    Page<Item> findByItemSellStatus(ItemSellStatus status, Pageable pageable);
    Page<Item> findByCategoryAndItemSellStatus(Category category, ItemSellStatus status, Pageable pageable);

    // 상세 조회: 리뷰만 즉시 로딩(EntityGraph)
    @EntityGraph(attributePaths = {"reviews"})
    @Query("SELECT DISTINCT i FROM Item i WHERE i.id = :id")
    Optional<Item> findWithReviewsById(@Param("id") Long id);

    // 상세 조회: 리뷰 + 작성자까지 즉시 로딩(EntityGraph)
    @EntityGraph(attributePaths = {"reviews", "reviews.member"})
    @Query("SELECT DISTINCT i FROM Item i WHERE i.id = :itemId")
    Optional<Item> findItemWithAllReviewsAndAuthors(@Param("itemId") Long itemId);
}
