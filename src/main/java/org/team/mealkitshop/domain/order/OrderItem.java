package org.team.mealkitshop.domain.order;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseTimeEntity;
import org.team.mealkitshop.domain.item.Item;

@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId; // PK

    // 주문 (다대일)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 상품 (다대일)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // 수량
    @Column(nullable = false)
    private int quantity;

    /**
     * 해당 주문 라인의 총 금액 (판매가 × 수량)
     */
    public int getLineTotal() {
        return item.getPrice() * quantity;
    }

    /**
     * 해당 주문 라인의 할인 금액
     * - 현재 구조에서는 상품 자체 할인은 없고, 등급 할인만 서비스에서 처리
     * - 따라서 항상 0을 반환
     */
    public int getLineDiscount() {
        return 0;
    }

}
