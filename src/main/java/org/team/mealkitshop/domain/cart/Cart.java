package org.team.mealkitshop.domain.cart;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.domain.member.Member;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart", uniqueConstraints = @UniqueConstraint(columnNames = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = {"member", "items"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cart extends BaseTimeEntity {

    // ✅ 무료배송 기준, 기본 배송비, 제주도 추가비용
    private static final int FREE_SHIPPING_THRESHOLD = 50_000;
    private static final int BASE_SHIPPING_FEE = 3_000;
    private static final int JEJU_EXTRA_FEE = 5_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long cartId;

    // ✅ 회원 1명당 장바구니 1개 (1:1 관계)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    // ✅ 장바구니 항목 리스트 (1:N 관계, 자동 저장/삭제)
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("cartItemId ASC")
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    // ✅ 생성 메서드
    public static Cart createFor(Member member) {
        Cart cart = new Cart();
        cart.member = member;
        cart.items = new ArrayList<>();
        return cart;
    }

    // ✅ 항목 추가 (양방향 관계도 함께 연결)
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    // ✅ 항목 제거 (양방향 관계 해제)
    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    // ✅ 모든 항목 제거
    public void clearItems() {
        for (CartItem item : new ArrayList<>(items)) {
            removeItem(item);
        }
    }

    // ✅ 전체 상품 총액
    public int getProductsTotal() {
        return items.stream()
                .mapToInt(CartItem::getLineTotal)
                .sum();
    }

    // ✅ 전체 할인 총액
    public int getDiscountTotal() {
        return items.stream()
                .mapToInt(CartItem::getLineDiscountTotal)
                .sum();
    }

    // ✅ 체크된 상품 총액
    public int getCheckedProductsTotal() {
        return items.stream()
                .filter(CartItem::isChecked)
                .mapToInt(CartItem::getLineTotal)
                .sum();
    }

    // ✅ 체크된 할인 총액
    public int getCheckedDiscountTotal() {
        return items.stream()
                .filter(CartItem::isChecked)
                .mapToInt(CartItem::getLineDiscountTotal)
                .sum();
    }

    // ✅ 배송비 계산 (zipcode로 제주도 여부 판단)
    public int getEstimatedShippingFee(String zipcode) {
        int subtotal = Math.max(0, getProductsTotal() - getDiscountTotal());

        int fee = (subtotal >= FREE_SHIPPING_THRESHOLD) ? 0 : BASE_SHIPPING_FEE;

        if (zipcode != null && zipcode.startsWith("63")) {
            fee += JEJU_EXTRA_FEE;
        }

        return fee;
    }

    // ✅ 기본 배송비 계산 (우편번호 없이)
    public int getEstimatedShippingFee() {
        return getEstimatedShippingFee(null);
    }

    // ✅ 최종 결제금액 (전체 항목 기준)
    public int getPayableAmount(String zipcode) {
        int subtotal = Math.max(0, getProductsTotal() - getDiscountTotal());
        return subtotal + getEstimatedShippingFee(zipcode);
    }

    // ✅ 최종 결제금액 (체크된 항목 기준)
    public int getCheckedPayableAmount(String zipcode) {
        int subtotal = Math.max(0, getCheckedProductsTotal() - getCheckedDiscountTotal());

        int fee = (subtotal >= FREE_SHIPPING_THRESHOLD) ? 0 : BASE_SHIPPING_FEE;

        if (zipcode != null && zipcode.startsWith("63")) {
            fee += JEJU_EXTRA_FEE;
        }

        return subtotal + fee;
    }
}
