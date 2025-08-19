package org.team.mealkitshop.repository.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.team.mealkitshop.domain.order.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 페이징 - 회원별 최신순 (주소 + 아이템까지 로딩)
    @EntityGraph(attributePaths = {"address", "orderItems", "orderItems.item"})
    Page<Order> findByMember_MnoOrderByOrderDateDesc(Long mno, Pageable pageable);

    // 전체 목록 - 회원별 최신순
    @EntityGraph(attributePaths = {"address", "orderItems", "orderItems.item"})
    List<Order> findByMember_MnoOrderByOrderDateDesc(Long mno);

    // 단건 상세 (본인 것만)
    @EntityGraph(attributePaths = {"address", "orderItems", "orderItems.item"})
    Optional<Order> findByOrderIdAndMember_Mno(Long orderId, Long mno);
}
