package org.team.mealkitshop.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardImageDTO {
    // BoardImage 엔티티를 프론트로 보내는 DTO

    private String uuid;
    private String fileName;
    private int ord; // 이미지 순서정보

}
