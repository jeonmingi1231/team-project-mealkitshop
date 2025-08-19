package org.team.mealkitshop.dto.item;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MainItemDTO {
    //메인화면에서 보여지는 상품에 대한 DTO

    private Long id; // 상품 고유 번호

    private String itemNm; // 상품명

    private String itemDetail; // 상품 설명

    private String imgUrl; // 상품 대표 이미지

    private Integer price; // 가격

    private Long itemLike; // 상품 찜, 관심 수

    private Long itemViewCnt; // 상품 조회 수

    @QueryProjection
    public MainItemDTO(Long id, String itemNm, String itemDetail, String imgUrl,Integer price,
                       Long itemLike, Long itemViewCnt) {
        this.id = id;
        this.itemNm = itemNm;
        this.itemDetail = itemDetail;
        this.imgUrl = imgUrl;
        this.price = price;
        this.itemLike = itemLike;
        this.itemViewCnt = itemViewCnt;
    }
}
