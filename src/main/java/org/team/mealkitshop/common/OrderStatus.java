package org.team.mealkitshop.common;

public enum OrderStatus {
    CREATED,   // 주문 생성(결제 전 or 결제요청 직후)
    PAID,      // 결제 완료
    PREPARING, // 상품 준비중
    SHIPPED,   // 출고/배송중
    DELIVERED, // 배송완료
    COMPLETED, // 구매확정
    CANCELED   // 주문 취소
}
