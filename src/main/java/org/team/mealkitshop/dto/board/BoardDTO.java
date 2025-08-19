package org.team.mealkitshop.dto.board;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.team.mealkitshop.common.BaseTimeEntity;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class BoardDTO extends BaseTimeEntity {

    private Long bno;                   // 게시글 번호

    @NotEmpty // 빈 문자열이나 null 허용x
    @Size(min = 5, max = 30)
    private String title;               // 제목

    @NotEmpty
    private String content;             // 내용

    @NotEmpty
    private String writer;              // 작성자

    private LocalDateTime regDate;  // DTO용, 엔티티가 insert 시 처리
    private LocalDateTime modDate;  // DTO용, 엔티티가 insert 시 처리

    @Builder.Default
    private boolean secretBoard = false;        // 비밀글 여부

    private String secretPassword;      // 비밀글에서 사용할 비밀번호

    private List<String> fileNames; // 첨부파일 목록
    // 리포지토리에서 처리는 엔티티는
    // private Set<BoardAttachment> AttachmentSet = new HashSet<BoardAttachment>();
    // dto를 엔티티로 변환하는 ModelMapper를 사용했었는데 다양한 처리를 위해서 custom
    // -> 서비스 계층에서 처리

}
