package org.team.mealkitshop.dto.board;

import lombok.*;
import org.team.mealkitshop.common.BoardReactionType;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardReactionDTO {
    // 좋아요, 추천, 비추천

    private Long boardId;

    private String userId;

    private BoardReactionType reaction;
    // BoardReactionType > enum (LIKE, DIS_LIKE, UP_VOTE, DOWN_VOTE)

}
