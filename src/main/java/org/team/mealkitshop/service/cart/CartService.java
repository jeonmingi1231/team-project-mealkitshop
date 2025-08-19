package org.team.mealkitshop.service.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.domain.cart.Cart;
import org.team.mealkitshop.domain.cart.CartItem;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.cart.*;
import org.team.mealkitshop.repository.cart.CartItemRepository;
import org.team.mealkitshop.repository.cart.CartRepository;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    /* ============================== */
    /*         내부 유틸 메서드         */
    /* ============================== */

    // [1] 회원의 장바구니를 조회하거나, 없으면 새로 생성해서 반환
    private Cart getOrCreateCartByMemberMno(Long mno) {
        Optional<Cart> foundCart = cartRepository.findByMember_Mno(mno);
        if (foundCart.isPresent()) {
            return foundCart.get(); // 이미 있으면 반환
        } else {
            // 없으면 해당 회원을 찾아서
            Member member = memberRepository.findById(mno)
                    .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + mno));
            // 새로운 장바구니 생성
            Cart newCart = Cart.createFor(member);
            return cartRepository.save(newCart);
        }
    }

    // [2] 회원의 장바구니 안에 있는 특정 항목(cartItem)을 가져오기 (권한 체크 포함)
    private CartItem getOwnedCartItem(Long memberMno, Long cartItemId) {
        Cart cart = cartRepository.findByMember_Mno(memberMno)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원의 장바구니가 없습니다. memberId=" + memberMno));
        return cartItemRepository.findByCartAndCartItemId(cart, cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원 장바구니에 이 항목이 없습니다. cartItemId=" + cartItemId));
    }

    // [3] 회원 등급에 따라 쿠폰 할인율 설정
    private int getGradeCouponPercent(Grade grade) {
        if (grade == null) return 0;
        if (grade == Grade.VIP) return 10;
        if (grade == Grade.GOLD) return 7;
        if (grade == Grade.SILVER) return 5;
        return 0;
    }

    // [4] 장바구니 항목 엔티티 → 프론트로 보낼 DTO로 변환
    private CartItemDto toDto(CartItem ci) {
        return CartItemDto.builder()
                .cartItemId(ci.getCartItemId())
                .itemId(ci.getItem().getId())
                .itemName(ci.getItem().getItemNm())
                .listPrice(ci.getItem().getPrice())
                .quantity(ci.getQuantity())
                .checked(ci.isChecked())
                .lineTotal(ci.getLineTotal())
                .lineDiscountTotal(ci.getLineDiscountTotal())
                .linePayable(ci.getLinePayable())
                .thumbnailUrl(null)
                .build();
    }

    // [5] 장바구니 전체에 대한 요약 정보 생성 (쿠폰 포함)
    private AmountSummary toAllSummaryWithCoupon(Cart cart, Member member, String zipcode) {
        int productsTotal = cart.getProductsTotal();
        int discountTotal = cart.getDiscountTotal();
        int subtotal      = Math.max(0, productsTotal - discountTotal);
        int shippingFee   = cart.getEstimatedShippingFee(zipcode);

        Grade grade = (member != null) ? member.getGrade() : null;
        int percent = getGradeCouponPercent(grade);
        int couponDiscount = Math.min((int) Math.round(subtotal * (percent / 100.0)), subtotal);
        int payableAmount  = Math.max(0, subtotal + shippingFee - couponDiscount);

        return AmountSummary.builder()
                .productsTotal(productsTotal)
                .discountTotal(discountTotal)
                .shippingFee(shippingFee)
                .payableAmount(payableAmount)
                .couponDiscount(couponDiscount)
                .appliedCouponCode(grade != null ? grade.name() : null)
                .build();
    }

    // [6] 체크된 항목만 대상으로 요약 정보 생성 (쿠폰 포함)
    private AmountSummary toCheckedSummaryWithCoupon(Cart cart, Member member, String zipcode) {
        int products = cart.getCheckedProductsTotal();
        int discount = cart.getCheckedDiscountTotal();
        int subtotal = Math.max(0, products - discount);
        int payable  = cart.getCheckedPayableAmount(zipcode);
        int fee      = Math.max(0, payable - subtotal);

        Grade grade = (member != null) ? member.getGrade() : null;
        int percent = getGradeCouponPercent(grade);
        int coupon  = Math.min((int) Math.round(subtotal * (percent / 100.0)), subtotal);
        int finalPayable = Math.max(0, subtotal + fee - coupon);

        return AmountSummary.builder()
                .productsTotal(products)
                .discountTotal(discount)
                .shippingFee(fee)
                .payableAmount(finalPayable)
                .couponDiscount(coupon)
                .appliedCouponCode(grade != null ? grade.name() : null)
                .build();
    }

    /* ============================== */
    /*          공개 서비스 메서드      */
    /* ============================== */

    // [A] 상품을 장바구니에 추가
    public Long addToCart(Long memberMno, AddToCartRequest req) {
        Cart cart = getOrCreateCartByMemberMno(memberMno);

        Item item = itemRepository.findById(req.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. id=" + req.getItemId()));

        Optional<CartItem> found = cartItemRepository.findByCartAndItem(cart, item);
        if (found.isPresent()) {
            CartItem line = found.get();
            int delta = Math.max(1, req.getQuantity());
            line.increaseQuantity(delta);
            return line.getCartItemId();
        }

        CartItem created = CartItem.builder()
                .item(item)
                .quantity(Math.max(1, req.getQuantity()))
                .checked(true)
                .build();

        cart.addItem(created);
        return cartItemRepository.save(created).getCartItemId();
    }

    // [B] 장바구니 항목 수정 (수량 변경 / 체크 여부)
    public void updateCartItem(Long memberMno, UpdateCartItemRequest req) {
        CartItem line = getOwnedCartItem(memberMno, req.getCartItemId());

        if (req.getQuantity() != null) {
            int q = req.getQuantity();
            if (q <= 0) {
                Cart owner = line.getCart();
                cartItemRepository.delete(line);
                cartItemRepository.flush();
                owner.getItems().removeIf(ci -> ci.getCartItemId().equals(line.getCartItemId()));
                return;
            } else {
                line.changeQuantity(q);
            }
        }

        if (req.getChecked() != null) {
            boolean target = req.getChecked();
            if (line.isChecked() != target) {
                line.toggleChecked();
            }
        }
    }

    // [C] 장바구니 조회 (상품 목록 + 금액 요약 포함)
    @Transactional(readOnly = true)
    public CartDetailResponse getCartDetail(Long memberMno, String zipcode) {
        Cart cart = cartRepository.findByMember_Mno(memberMno).orElse(null);

        if (cart == null) {
            Member member = memberRepository.findById(memberMno).orElse(null);
            String applied = (member != null && member.getGrade() != null) ? member.getGrade().name() : null;

            AmountSummary empty = AmountSummary.builder()
                    .productsTotal(0)
                    .discountTotal(0)
                    .shippingFee(0)
                    .payableAmount(0)
                    .couponDiscount(0)
                    .appliedCouponCode(applied)
                    .build();

            return CartDetailResponse.builder()
                    .cartId(null)
                    .memberId(memberMno)
                    .items(List.of())
                    .summary(empty)
                    .checkedSummary(empty)
                    .build();
        }

        List<CartItemDto> items = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            items.add(toDto(ci));
        }

        Member member = cart.getMember();
        AmountSummary summary = toAllSummaryWithCoupon(cart, member, zipcode);
        AmountSummary checkedSummary = toCheckedSummaryWithCoupon(cart, member, zipcode);

        return CartDetailResponse.builder()
                .cartId(cart.getCartId())
                .memberId(memberMno)
                .items(items)
                .summary(summary)
                .checkedSummary(checkedSummary)
                .build();
    }

    // [D] 장바구니 전체 비우기
    public void clearCart(Long memberMno) {
        // (1) 장바구니 존재 확인
        Cart cart = cartRepository.findByMember_Mno(memberMno)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원 장바구니가 없습니다. id=" + memberMno));

        // (2) DB에서 항목 전체 삭제
        cartItemRepository.deleteAll(cart.getItems());

        // (3) 메모리상 리스트도 비움
        cart.getItems().clear();
    }
}
