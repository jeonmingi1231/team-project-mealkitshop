package org.team.mealkitshop.domain.item;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseEntity;
import org.team.mealkitshop.common.Category;
import org.team.mealkitshop.common.FoodItem;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.domain.cart.CartItem;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id; // 상품코드 (PK)

    @Column(length = 50, nullable = false)
    private String itemNm; // 상품명

    @Column(nullable = false)
    private int price; // 가격

    @Column(nullable = false)
    private int stockNumber; // 재고수

    @Lob
    @Column(nullable = false)
    private String itemDetail; // 상세 설명

    @Enumerated(EnumType.STRING)
    private ItemSellStatus itemSellStatus; // 판매상태

    /** 장바구니 항목들 (편의 메서드 제외) **/
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> cartitems = new ArrayList<>();

    /** 분류 (대분류/중분류) **/
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) // 정책에 따라 required면 false로 변경
    private Category category; // 냉장/냉동/기타 (대분류)

    @Enumerated(EnumType.STRING)
    private FoodItem foodItem; // SALAD, DRESSING 등 (중분류)

    /** 지표 **/
    @Builder.Default
    @Column(nullable = false)
    private long itemLike = 0L;       // 좋아요 수

    @Builder.Default
    @Column(nullable = false)
    private long itemViewCnt = 0L;    // 조회 수

    /** 리뷰 컬렉션 **/
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    /** 이미지 컬렉션 **/
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemImage> images = new ArrayList<>();

    /* ===================== 편의 메서드 (양방향 동기화) ===================== */

    // Review <-> Item
    public void addReview(Review review) {
        this.reviews.add(review);
        review.setItem(this);
    }

    public void removeReview(Review review) {
        this.reviews.remove(review);
        review.setItem(null);
    }

    // ItemImage <-> Item
    public void addImage(ItemImage image) {
        this.images.add(image);
        image.setItem(this);
    }

    public void removeImage(ItemImage image) {
        this.images.remove(image);
        image.setItem(null);
    }

    /* ===================== 분류 동기화(가드) ===================== */

    /**
     * 외부에서 category를 세팅하더라도, foodItem이 이미 정해져 있으면
     * 최종 값은 항상 foodItem의 category로 맞춥니다.
     */
    @Deprecated
    protected void setCategory(Category category) {
        // 도메인 규칙: category는 항상 foodItem에서 파생
        this.category = category;
    }


    /**
     * 실질적인 소스는 foodItem.
     * foodItem이 변경될 때마다 category를 즉시 동기화합니다.
     */
    public void setFoodItem(FoodItem foodItem) {
        this.foodItem = foodItem;
        this.category = (foodItem != null) ? foodItem.getCategory() : null;
    }
    /** 저장/업데이트 직전, 최종적으로 한 번 더 동기화하여 일관성 보장 */
    @PrePersist
    @PreUpdate
    private void syncCategory() {
        this.category = (foodItem != null) ? foodItem.getCategory() : null;
    }

    public Category getCategory() {return this.category;}

    /* ===================== 기타 유틸 ===================== */
    public void updateItem(org.team.mealkitshop.dto.item.ItemFormDTO dto) {
        this.itemNm = dto.getItemNm();
        this.price = dto.getPrice();
        this.stockNumber = dto.getStockNumber();
        this.itemDetail = dto.getItemDetail();
        this.itemSellStatus = dto.getItemSellStatus();
        setFoodItem(dto.getFoodItem()); // ← category 자동 동기화
    }
    public void increaseLike()    { this.itemLike++; }
    public void decreaseLike()    { if (this.itemLike > 0) this.itemLike--; }
    public void increaseViewCnt() { this.itemViewCnt++; }
}
