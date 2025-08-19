package org.team.mealkitshop.common;

public enum OrderStatus {
    CREATED,   // 생성(결제 전)
    PAID,      // 결제완료
    SHIPPED,   // 배송중
    DELIVERED, // 배송완료
    CANCELLED  // 취소(전체/부분취소는 추가 설계)
}
