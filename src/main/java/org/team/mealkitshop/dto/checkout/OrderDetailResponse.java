package org.team.mealkitshop.dto.checkout;

import lombok.*;
import org.team.mealkitshop.common.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailResponse {
// 👉 주문 상세 화면 또는 결제 완료 화면에서 필요한 데이터 전체를 담는 객체

    /**
     * 주문 고유 ID (PK)
     * - DB에서 주문을 구분하는 기본키
     */
    private Long orderId;

    /**
     * 주문 번호
     * - 화면과 영수증에 표시되는 번호
     * - 예: 20250810-0001
     */
    private String orderNo;

    /**
     * 주문한 회원 ID (Member.mno)
     */
    private Long memberMno;

    /**
     * 주문 상품 목록
     * - OrderDetailLine 객체들을 담는 리스트
     * - 각 OrderDetailLine이 "상품 1줄"에 해당
     */
    private List<OrderDetailLine> lines;

    // 💰 금액 요약
    /**
     * 상품금액 합계 (판매가 기준)
     */
    private int productsTotal;

    /**
     * 총 할인액
     * - (정가 - 판매가) 합계
     */
    private int discountTotal;

    /**
     * 배송비
     */
    private int shippingFee;

    /**
     * 최종 결제금액
     * - productsTotal - discountTotal + shippingFee
     */
    private int payableAmount;

    // 📦 주문 상태 및 시간
    /**
     * 주문 상태
     * - 예: CREATED(생성), PAID(결제 완료), SHIPPED(배송 중), COMPLETED(완료)
     */
    private OrderStatus status;

    /**
     * 주문 시각
     * - 주문이 접수된 날짜·시간
     */
    private LocalDateTime orderedAt;

    // 🚚 배송 및 수취인 정보
    /**
     * 수취인 이름
     */
    private String receiverName;

    /**
     * 수취인 연락처
     */
    private String receiverPhone;

    /**
     * 배송지 우편번호
     */
    private String zipcode;

    /**
     * 기본 주소
     */
    private String addr1;

    /**
     * 상세 주소
     */
    private String addr2;

    /**
     * 배송 메모 (선택)
     * - 예: "부재 시 경비실에 맡겨주세요"
     */
    private String memo;
}

//역할
//주문 상세 화면(또는 결제 완료 화면)에 필요한 모든 데이터를 한 번에 제공
//상품 목록, 금액 합계, 배송 정보, 주문 상태 등을 모두 포함
//구성
//기본 정보: orderId, orderNo, memberMno
//상품 목록: List<OrderDetailLine> (각 상품 한 줄 정보)
//금액 요약: productsTotal, discountTotal, shippingFee, payableAmount
//상태/시간: status, orderedAt
//배송 정보: receiverName, receiverPhone, zipcode, address1, address2, memo

//사용 흐름
//Controller에서 주문 ID를 받아 Service 호출
//Service에서 주문 + 주문상품 + 배송 정보를 조회
//DTO에 채워서 프론트에 전달
//프론트는 이 DTO 하나로 전체 주문 상세 화면을 렌더링