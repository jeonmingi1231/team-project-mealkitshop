package org.team.mealkitshop.domain.item;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.team.mealkitshop.common.BaseEntity;
import org.team.mealkitshop.domain.member.Member;

@Entity
@Table(name = "review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"member", "item"})
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 리뷰 고유 번호

    // 리뷰 작성자 (회원) - mno PK 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mno", nullable = false) // FK: review.mno
    private Member member;

    // 리뷰 대상 상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, length = 500)
    private String content; // 리뷰 내용

    @Column(nullable = false)
    @Min(1)
    @Max(5)
    private int rating; // 평점 (1~5)


}
