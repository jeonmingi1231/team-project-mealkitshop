package org.team.mealkitshop.dto.board;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReplyDTO {
    // 댓글
    // rest방식의 객체 처리용

    private Long rno;   // 댓글용 번호

    @NotNull // 필수값 (Null만 허용하지 않는다) -> "", " "은 허용됨
    private Long bno;   // 게시글에 fk용

    @NotEmpty // Null, "" 까지 허용하지 않음. -> " "허용됨 -> @NotBlank " " 차단
    private String replyText; // 댓글내용

    //https://sanghye.tistory.com/36

    @NotEmpty
    private String replyer; // 댓글 작성자

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regDate;

    @JsonIgnore
    private LocalDateTime modDate; // 등록일, 수정일

}
