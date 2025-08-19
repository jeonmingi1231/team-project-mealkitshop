package org.team.mealkitshop.domain.item;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "item")
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Entity
@Table(name = "item_img", indexes = { @Index(name = "idx_itemimg_item", columnList = "item_id") })
public class ItemImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_img_id")
    @EqualsAndHashCode.Include
    private Long id;

    /** 저장 파일명 (서버에 저장되는 이름) */
    @Column(nullable = false, length = 255)
    private String imgName;

    /** 원본 파일명 (업로드 당시 이름) */
    @Column(nullable = false, length = 255)
    private String oriImgName;

    /** 공개 경로 (/images/...) */
    @Column(nullable = false, length = 500)
    private String imgUrl;

    /**
     * 대표 이미지 여부 (Y/N 컨버터 autoApply)
     * - DB: CHAR(1) 'Y'/'N'
     * - Java: Boolean (null 허용) → 저장 직전에 false로 보정
     */
    @Column(name = "repimg_yn", nullable = false, length = 1)
    private Boolean repimgYn; // 필드 기본값 대입 없음 → 경고 없음

    /** 소속 상품 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** 파일명/경로 갱신 편의 메서드 */
    public void updateItemImg(String oriImgName, String imgName, String imgUrl) {
        this.oriImgName = oriImgName;
        this.imgName = imgName;
        this.imgUrl = imgUrl;
    }

    /** 저장 직전 null 방지 보정(기본 false) */
    @PrePersist
    private void prePersist() {
        if (this.repimgYn == null) {
            this.repimgYn = Boolean.FALSE;
        }
    }
}
