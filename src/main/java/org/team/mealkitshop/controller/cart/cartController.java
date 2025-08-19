package org.team.mealkitshop.controller.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.dto.cart.AddToCartRequest;
import org.team.mealkitshop.dto.cart.UpdateCartItemRequest;
import org.team.mealkitshop.dto.cart.CartDetailResponse;
import org.team.mealkitshop.service.cart.CartService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class cartController {

    private final CartService cartService;

    /** 장바구니 조회 페이지 */
    @GetMapping("/{memberId}")
    public String viewCart(@PathVariable Long memberId,
                           @RequestParam(defaultValue = "00000") String zipcode,
                           Model model) {

        CartDetailResponse cart = cartService.getCartDetail(memberId, zipcode);
        model.addAttribute("cart", cart);
        return "cart/view"; // ⇒ templates/cart/view.html
    }

    /** 장바구니 상품 추가 */
    @PostMapping("/{memberId}/add")
    public String addToCart(@PathVariable Long memberId,
                            @ModelAttribute AddToCartRequest request) {
        cartService.addToCart(memberId, request);
        return "redirect:/cart/" + memberId;
    }

    /** 장바구니 항목 수정 (수량/체크박스) */
    @PostMapping("/{memberId}/update")
    public String updateCartItem(@PathVariable Long memberId,
                                 @ModelAttribute UpdateCartItemRequest request) {
        cartService.updateCartItem(memberId, request);
        return "redirect:/cart/" + memberId;
    }

    /** 장바구니 전체 비우기 */
    @PostMapping("/{memberId}/clear")
    public String clearCart(@PathVariable Long memberId) {
        cartService.clearCart(memberId);
        return "redirect:/cart/" + memberId;
    }
}
