package org.team.mealkitshop.dto.item;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.team.mealkitshop.domain.item.ItemImage;

@Getter
@Setter
@Builder
public class ItemImgDTO {

    private Long id;

    private String imgName;   // 파일명
    private String oriImgName;// 원본명
    private String imgUrl;    // 경로

    /** 엔티티와 동일한 이름으로 통일: repimgYn */
    private Boolean repimgYn; // 대표 이미지 여부

    private static final ModelMapper modelMapper = new ModelMapper();

    // 엔티티 -> DTO 변환
    public static ItemImgDTO of(ItemImage itemImage) {
        return modelMapper.map(itemImage, ItemImgDTO.class);
    }
}
