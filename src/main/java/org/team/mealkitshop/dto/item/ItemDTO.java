package org.team.mealkitshop.dto.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.team.mealkitshop.common.Category;
import org.team.mealkitshop.common.FoodItem;
import org.team.mealkitshop.common.ItemSellStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class ItemDTO {

        private Long id;                // 상품 아이디
        private String itemNm;          // 상품명
        private Integer price;          // 가격
        private Integer stockNumber;    // 재고수 (추가)
        private String itemDetail;      // 상세설명

        private ItemSellStatus itemSellStatus; // 판매 상태
        private Category category;             // 대분류
        private FoodItem foodItem;             // 중분류

        private Long itemLike;         // 좋아요 수 (추가)
        private Long itemViewCnt;      // 조회 수 (추가)

        private double avgRating;    // 평균 평점
        private long reviewCount;    // 리뷰 개수


        // BaseTimeEntity
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime regTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updateTime;

        //  BaseEntity
        private String createdBy;      // 생성자
        private String modifiedBy;     // 최종 수정자
}
