// src/main/java/org/team/mealkitshop/repository/item/ReviewRepository.java
package org.team.mealkitshop.repository.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.item.Review;

import java.util.Collection;
import java.util.List;
import java.util.Map;


@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 특정 아이템의 리뷰 목록 (회원까지 페치) - 페이징 */
    @EntityGraph(attributePaths = {"member"}, type = EntityGraph.EntityGraphType.LOAD)
    Page<Review> findByItem_Id(Long itemId, Pageable pageable);

    /** 특정 아이템의 리뷰 + 이미지까지 페치 (N+1 방지) - 페이징 */
    @EntityGraph(attributePaths = {"member", "images"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("select r from Review r where r.item.id = :itemId")
    Page<Review> findWithImagesByItemId(@Param("itemId") Long itemId, Pageable pageable);

    /** 특정 아이템의 상위 평점 리뷰 10건 (회원까지 페치) */
    @EntityGraph(attributePaths = {"member"})
    List<Review> findTop10ByItem_IdOrderByRatingDescIdDesc(Long itemId);

    /** 특정 회원이 작성한 리뷰 목록 (회원 정보 포함) - 페이징 */
    @EntityGraph(attributePaths = {"member"}, type = EntityGraph.EntityGraphType.LOAD)
    Page<Review> findByMember_Mno(Long mno, Pageable pageable);

    /** 특정 아이템의 평균 평점 (Double, null일 경우 0으로 처리) */
    @Query("select coalesce(avg(r.rating), 0) from Review r where r.item.id = :itemId")
    Double getAverageRatingByItemId(@Param("itemId") Long itemId);

    /** 특정 아이템의 평균 평점 (기본형 double 반환) */
    @Query("select coalesce(avg(r.rating), 0) from Review r where r.item.id = :itemId")
    double getAverageRatingByItemIdAsPrimitive(@Param("itemId") Long itemId);

    /** 특정 아이템의 리뷰 개수 */
    long countByItem_Id(Long itemId);

    /** 특정 회원이 해당 아이템에 이미 리뷰 작성했는지 여부 */
    boolean existsByMember_MnoAndItem_Id(Long mno, Long itemId);

    /** 특정 아이템의 리뷰 일괄 삭제 (영속성 컨텍스트 반영 포함) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Review r where r.item.id = :itemId")
    void deleteByItemId(@Param("itemId") Long itemId);

    /** 특정 아이템의 리뷰 일괄 삭제 (단순 Bulk) */
    @Modifying
    @Query("delete from Review r where r.item.id=:itemId")
    void deleteByItemIdBulk(Long itemId);

    /** 특정 아이템의 평균 평점 (nullable Double 반환, coalesce 없음) */
    @Query("select avg(r.rating) from Review r where r.item.id = :itemId")
    Double findAverageRatingByItemId(@Param("itemId") Long itemId);

    /** 특정 아이템의 리뷰 개수 (Long 반환) */
    @Query("select count(r.id) from Review r where r.item.id = :itemId")
    Long countByItemId(@Param("itemId") Long itemId);

    /** 여러 아이템의 평균 평점 (itemId 기준 group by) */
    @Query("""
           select r.item.id as itemId, avg(r.rating) as avgRating
             from Review r
            where r.item.id in :itemIds
            group by r.item.id
           """)
    List<Map<String, Object>> findAvgRatingByItemIds(@Param("itemIds") Collection<Long> itemIds);

    /** 여러 아이템의 리뷰 개수 (itemId 기준 group by) */
    @Query("""
           select r.item.id as itemId, count(r.id) as reviewCount
             from Review r
            where r.item.id in :itemIds
            group by r.item.id
           """)
    List<Map<String, Object>> findReviewCountByItemIds(@Param("itemIds") Collection<Long> itemIds);
}
