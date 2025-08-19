package org.team.mealkitshop.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.dto.board.ReplyDTO;
import org.team.mealkitshop.dto.board.PageRequestDTO;
import org.team.mealkitshop.dto.board.PageResponseDTO;
import org.team.mealkitshop.service.ReplyService;

import java.util.HashMap;
import java.util.Map;

@RestController // 경로와 리턴이 다름
@RequestMapping("/replies") // http://localhost:80/replies
@Log4j2 //import lombok.extern.log4j.Log4j2;
@RequiredArgsConstructor // p555 추가
public class ReplyController {
    // 댓글용 컨틀롤러로 Rest방식으로 처리
    // 일반적으로 Contoller와 혼용해서 사용 됨

    private final ReplyService commentService;  // p555

    // C
//    @PostMapping(value="/" , consumes = MediaType.APPLICATION_JSON_VALUE)
//    //  post메서드로                                   json으로 처리용
//    public ResponseEntity<Map<String,Long>> register(@RequestBody ReplyDTO replyDTO){
//        // 리턴 타입이 응답용 객체에 key, value 로 전송하게 셋팅
//        //                                           프론트에서 ReplyDTO가 넘어오면 db에 전송
//            log.info(replyDTO); // 프론트에서 넘어온 객체를 콘솔에 출력
//            Map<String, Long> resultMap = Map.of("rno",111L);
//            //                                    key ,value
//
//        return ResponseEntity.ok(resultMap);  // 리턴타입이 프론트와 연결되지 않음.
//        //                   정상처리됨 (200) -> { "rno": 111 }
//    }

    // C
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Long> register(
            @Valid @RequestBody ReplyDTO boardCommentDTO, BindingResult bindingResult)throws BindException {
        // 검증용 코드   요청에 들어온 객게               리턴할 객체                   예외처리용

        log.info(boardCommentDTO);

        if(bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
            // 에러가 발생하면 처리하는 용도
        } // 예외처리 종료 if문

        Map<String,Long> resultMap = new HashMap<>();

        Long rno = commentService.register(boardCommentDTO);
        // 프론트에서 넘어온 dto 가 등록 (insert)처리됨
        // 결과는 댓글 번호가 넘어옴

        resultMap.put("rno",rno);

        // p556 제외resultMap.put("rno",111L); // 리턴 테스트용 코드
        //             key  value
        // Map에 객체 추가할 때는 put 메서드를 사용함

        return resultMap;  // 댓글번호가 프론트로 넘어간다.

        // keypoint : ReplyDTO를 프론트에서 수집할 때 @Valid를 적용 (null이나 ""등을 검증)
        //            null이나 ""가 들어오면 BindExceptin을 throw함
        //            메서드에 리턴값이 문제가 있으면 @RestControllerAdvice가 처리 함
        //            정상값만 리턴하게 셋팅
    }


    // R -> 전체
    @GetMapping(value = "/list/{bno}") // http://localhost/replies/list/게시글번호
    public PageResponseDTO<ReplyDTO> getList(
            @PathVariable("bno") Long bno, PageRequestDTO pageRequestDTO) {
        // URL에 있는 번호가 bno가 됨   , 페이징 처리용
        PageResponseDTO<ReplyDTO> responseDTO =
                commentService.getListOfBoard(bno, pageRequestDTO);

        return responseDTO;
    }
    // R -> 한개
    @GetMapping("/{rno}") // http://localhost/replies/댓글번호
    public ReplyDTO getBoardCommentDTO(@PathVariable("rno") Long rno) {

        ReplyDTO replyDTO = commentService.read(rno);
        // select * from reply where rno = rno
        return replyDTO;
    }

    // U
    @PutMapping(value="/{rno}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Long> remove(@PathVariable("rno") Long rno, @RequestBody ReplyDTO boardCommentDTO) {
        // remove 메서드 오버라이딩으로 이중화 처리
        boardCommentDTO.setRno(rno); // 댓글번호를 받아서 url로 받은 값과 일치
        commentService.modify(boardCommentDTO);  // 서비스 계층에서 update가 이루어짐
        Map<String,Long> resultMap = new HashMap<>();  // 결과전송용 객체 생성
        resultMap.put("rno",rno); // 수정된 댓글의 번호를 리턴
        return resultMap;
    }



    // D
    @DeleteMapping("/{rno}") // http://localhost/replies/댓글번호
    public Map<String,Long> remove(@PathVariable("rno") Long rno) {
        commentService.remove(rno);
        Map<String,Long> resultMap = new HashMap<>();
        resultMap.put("rno",rno);  //삭제된 게시글 번호를 리턴
        return resultMap; //  Map<"rno",게시글번호>
    }
}
