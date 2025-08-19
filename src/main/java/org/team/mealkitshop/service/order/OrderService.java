package org.team.mealkitshop.service.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.domain.cart.Cart;
import org.team.mealkitshop.domain.cart.CartItem;
import org.team.mealkitshop.domain.member.Address;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.domain.order.Order;
import org.team.mealkitshop.domain.order.OrderItem;
import org.team.mealkitshop.dto.checkout.CreateOrderRequest;
import org.team.mealkitshop.repository.address.AddressRepository;
import org.team.mealkitshop.repository.cart.CartItemRepository;
import org.team.mealkitshop.repository.cart.CartRepository;
import org.team.mealkitshop.repository.member.MemberRepository;
import org.team.mealkitshop.repository.order.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 서비스
 * - 장바구니 → 주문으로 변환
 * - 배송지는 Address 엔티티 직접 참조
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;

    /**
     * 주문 생성
     * - 결제 완료 시 호출
     */
    public Order createOrder(CreateOrderRequest req) {

        // 1) 회원 조회
        Member member = memberRepository.findById(req.getMemberMno())
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. mno=" + req.getMemberMno()));

        // 2) 장바구니 조회
        Cart cart = cartRepository.findByMember_Mno(req.getMemberMno())
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 없습니다. mno=" + req.getMemberMno()));

        // 3) 장바구니 항목 조회 (선택 결제 or 전체 결제)
        List<CartItem> cartItems = (req.getCartItemIds() != null && !req.getCartItemIds().isEmpty())
                ? cartItemRepository.findAllByCartAndCartItemIdIn(cart, req.getCartItemIds())
                : cartItemRepository.findAllByCart(cart);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("장바구니 항목이 없습니다.");
        }

        // 4) 배송지 조회
        Address shippingAddress = addressRepository.findById(req.getAddressId())
                .orElseThrow(() -> new IllegalArgumentException("배송지가 존재하지 않습니다. id=" + req.getAddressId()));

        // 5) 주문 엔티티 생성
        Order order = Order.builder()
                .orderNo(generateOrderNo())                // 주문번호 생성
                .member(member)                            // 주문자
                .address(shippingAddress)                  // 배송지 참조
                .orderDate(LocalDateTime.now())            // 주문 시각
                .status(org.team.mealkitshop.common.OrderStatus.CREATED)
                .productsTotal(0)
                .discountTotal(0)
                .shippingFee(0)
                .payableAmount(0)
                .receiverName(req.getReceiverName())
                .receiverPhone(req.getReceiverPhone())
                .build();

        // 6) 주문 상품 추가 + 금액 계산
        int productsTotal = 0; // 상품 총액
        for (CartItem ci : cartItems) {
            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .item(ci.getItem())
                    .quantity(ci.getQuantity())
                    .build();

            order.addItem(oi);

            // 판매가 × 수량
            productsTotal += ci.getItem().getPrice() * ci.getQuantity();
        }

        // 7) 배송비 정책
        int shippingFee = (productsTotal >= 50_000) ? 0 : 3_000;
        if (shippingAddress.getZipCode() != null && shippingAddress.getZipCode().startsWith("63")) {
            shippingFee += 5_000; // 제주/도서산간 추가
        }

        // 8) 회원 등급 할인
        int gradePercent = getGradeCouponPercent(member.getGrade());
        int gradeCouponDiscount = Math.max(0,
                Math.min((int) Math.round(productsTotal * (gradePercent / 100.0)), productsTotal));

        // 9) 최종 금액 설정
        order.setProductsTotal(productsTotal);
        order.setDiscountTotal(gradeCouponDiscount);
        order.setShippingFee(shippingFee);
        order.setPayableAmount(Math.max(0, (productsTotal - gradeCouponDiscount)) + shippingFee);

        // 10) 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 11) 장바구니 비우기
        cartItemRepository.deleteAll(cartItems);

        return savedOrder;
    }

    /** 회원 등급별 할인율 */
    private int getGradeCouponPercent(org.team.mealkitshop.common.Grade grade) {
        if (grade == null) return 0;
        return switch (grade) {
            case VIP -> 10;
            case GOLD -> 7;
            case SILVER -> 5;
            default -> 0;
        };
    }

    /** 주문번호 생성 (예: 20250812-ABCD1234) */
    private String generateOrderNo() {
        return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
