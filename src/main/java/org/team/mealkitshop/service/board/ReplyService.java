package org.team.mealkitshop.service.board;

import org.team.mealkitshop.dto.board.PageRequestDTO;
import org.team.mealkitshop.dto.board.PageResponseDTO;
import org.team.mealkitshop.dto.board.ReplyDTO;

public interface ReplyService {

        // 조장이 시그니쳐를 정하는 곳!!!

        // 등록용 -> 프론트에서 dto객체가 넘어오면 데이터베이스에 .save()
        Long register(ReplyDTO replyDTO);  // 리턴은 long -> rno가 나옴

        // 1개 보이는 용 -> 프론트에서 rno가 넘어오면 데이터베이스에 .findbyid()
        ReplyDTO read(Long rno);  // 객체를 리턴한다.

        // 여러개 보이는 용  -> 게시물에 번호가 넘어오면 댓글의 리스트가 나오면서 페이징기법이 적용
        PageResponseDTO<ReplyDTO> getListOfBoard(Long bno, PageRequestDTO pageRequestDTO);
        // 리턴은 페이징처리응답용 객체에 댓글 객체가 담겨 나온다. p554

        // 1개 수정용 -> 프론트에서 DTO객체가 넘어오면 .save() 메서드 처리함
        void modify(ReplyDTO replyDTO);

        // 1개 삭제용 -> 프론트에서 rno(댓글번호) 가 넘어오면 .deletebyid()가 실행된다.
        void remove(Long rno);
    }


