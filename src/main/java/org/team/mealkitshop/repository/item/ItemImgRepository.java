// src/main/java/org/team/mealkitshop/repository/item/ItemImgRepository.java
package org.team.mealkitshop.repository.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.item.ItemImage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemImgRepository extends JpaRepository<ItemImage, Long> {

    /** 특정 아이템의 모든 이미지 조회 (등록 순서대로) */
    List<ItemImage> findByItemIdOrderByIdAsc(Long itemId);

    /** 특정 아이템의 대표 이미지 1건 조회 */
    Optional<ItemImage> findTopByItemIdAndRepimgYnTrueOrderByIdAsc(Long itemId);

    /** 여러 아이템의 대표 이미지들 조회 */
    List<ItemImage> findByItem_IdInAndRepimgYnTrue(Collection<Long> itemIds);

    /** 여러 아이템의 전체 이미지 조회 (등록순) */
    List<ItemImage> findByItem_IdInOrderByIdAsc(Collection<Long> itemIds);

    /** 특정 아이템에 대표 이미지가 존재하는지 여부 확인 */
    boolean existsByItemIdAndRepimgYnTrue(Long itemId);

    /** 특정 아이템의 기존 대표 이미지 해제 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ItemImage i set i.repimgYn = false where i.item.id = :itemId and i.repimgYn = true")
    int clearRep(@Param("itemId") Long itemId);

    /** 특정 이미지를 대표 이미지로 지정 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ItemImage i set i.repimgYn = true where i.id = :imageId")
    int setRep(@Param("imageId") Long imageId);

}
