package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BoardReactionType;


@Entity
@Table(name = "board_reaction")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;        // PK

    private Long boardId;   // 게시글 ID(FK 역할)

    private String userId;  // 사용자 ID

    @Enumerated(EnumType.STRING)
    private BoardReactionType reaction; // 좋아요, 추천 등

}
