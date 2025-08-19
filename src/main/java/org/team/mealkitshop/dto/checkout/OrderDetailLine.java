package org.team.mealkitshop.dto.checkout;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailLine {
    // 👉 주문 상세 화면에 표시되는 "상품 한 줄"의 데이터
    //    한 주문에 여러 상품이 있을 경우, 각 상품마다 OrderDetailLine 하나씩 생성됩니다.

    /**
     * 주문 항목 고유 ID
     * - OrderItem 테이블의 PK
     * - 예: 301 → 301번 주문항목
     */
    private Long orderItemId;

    /**
     * 상품 고유 ID
     * - 원본 상품(Item)의 PK
     */
    private Long itemId;

    /**
     * 상품명 (스냅샷)
     * - 주문 시점의 상품명을 저장해둠
     * - 상품명이 나중에 바뀌더라도 주문 기록에는 당시 이름이 유지됨
     */
    private String itemName;

    /**
     * 정상가(정가)
     * - 할인 전 가격
     */
    private int listPrice;

    /**
     * 판매가
     * - 할인 적용 후 가격
     */
    private int salePrice;

    /**
     * 수량
     * - 해당 상품을 주문한 개수
     */
    private int quantity;

    /**
     * 총액
     * - 판매가 × 수량
     */
    private int lineTotal;

    /**
     * 총 할인액
     * - (정상가 - 판매가) × 수량
     */
    private int lineDiscount;
}

//역할: 주문 상세 페이지에서 상품 하나의 정보를 보여줄 때 사용.
//주요 필드:
//식별 정보: orderItemId, itemId
//상품 정보: itemName, listPrice, salePrice, quantity
//계산 값: lineTotal, lineDiscount

//사용 흐름:
//Service에서 OrderItem 엔티티를 조회 → 필요한 값들을 꺼내서 OrderDetailLine DTO에 채움
//Controller가 이 DTO 리스트를 반환
//프론트는 이 리스트를 테이블 형태로 렌더링
