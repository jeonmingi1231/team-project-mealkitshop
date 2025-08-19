package org.team.mealkitshop.dto.item;

import lombok.Getter;
import lombok.Setter;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.common.ItemSortType;

@Getter @Setter
public class ItemSearchDTO {
    // 상품 검색 조건에 대한 DTO

    private String searchDateType; // 등록일 기준 검색 (1d, 1w, 1m, 6m 등)
    private ItemSellStatus searchSellStatus; // 판매 상태
    private String searchBy; // 검색 유형 (itemNm, createdBy)
    private String searchQuery = ""; // 검색 키워드


    private ItemSortType sortType = ItemSortType.NEW; // 기본값 최신 순

    //  sortType 값 예시:
    //    POPULAR_LIKE,   찜 많은 순
    //    POPULAR_VIEW,  조회수 많은 순
    //    PRICE_ASC,     가격 낮은 순
    //    PRICE_DESC,    가격 높은 순
    //    NEW            최신순
    //}
}
