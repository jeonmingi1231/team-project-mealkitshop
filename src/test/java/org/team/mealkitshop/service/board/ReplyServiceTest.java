package org.team.mealkitshop.service.board;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.team.mealkitshop.dto.board.ReplyDTO;

@SpringBootTest
@Log4j2
class ReplyServiceTest {

    @Autowired
    private ReplyService replyService;

    @Test
    public void testRegister() {
        // 프론트에서 dto가 넘어오면 댓글 db에 등록 insert

        ReplyDTO replyDTO = ReplyDTO.builder()
                .replyText("서비스에서 댓글 등록 테스트")
                .replyer("서비스 테스트")
                .bno(2L) // 2번 게시물에 댓글 등록 연습
                .build();

        log.info("testRegister() 메서드 실행");
        log.info(replyService.register(replyDTO));

    }

}