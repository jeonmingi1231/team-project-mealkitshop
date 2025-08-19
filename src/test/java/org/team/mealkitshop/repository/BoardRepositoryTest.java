package org.team.mealkitshop.repository;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.board.BoardImage;
import org.team.mealkitshop.dto.board.BoardListReplyCountDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest // 메서드용 테스트 동작
@Transactional
@Commit
@Log4j2 // 로그용
class BoardRepositoryTest {

    // 영속성 계층에 테스트용

    @Autowired // 생성자 자동 주입
    private BoardRepository boardRepository;

    @Autowired
    private ReplyRepository replyRepository;

    @Test
    public void testInsert(){
        // 데이터베이스에 데이터 주입(c) 테스트 코드
        // 비밀게시글 Y/N 둘 다 정상 작동

        IntStream.rangeClosed(1,2).forEach(i -> {
                    // i 변수에 1~20까지 20개의 정수를 반복해서 생성
                    Board board = Board.builder()
                            .title("제목..."+i)  // board.setTitle()
                            .content("내용..."+i) // board.setContent()
                            .writer("user"+(i%10))  // board.setWriter()
                            .secretBoard(true)
                            .secretPassword("1234")
                            .build(); // @Builder 용 (세터 대신 좀더 간단하고 가독성 좋게 )
                    // log.info((board));
                    Board result = boardRepository.save(board) ; // 데이터베이스에 기록하는 코드
                    //                            .save 메서드는 jpa에서 상속한 메서드로 값을 저장하는 용도
                    //                                          이미 값이 있으면 update를 진행한다.
                    log.info("게시물 번호 출력 : " + result.getBno() + "게시물의 제목 : " + result.getTitle());

                }// forEach문 종료
        );// IntStream. 종료

    } // testInsert 메서드 종료

    @Test
    public void testSelect(){
        Long bno = 20L; // 게시물 번호가 20인 개체를 확인 해보자.

        Optional<Board> result = boardRepository.findById(bno);

        Board board = result.orElseThrow(); // 값이 있으면 넣어라

        log.info(bno + "가 데이터 베이스에 존재합니다. ");
        log.info(board) ; // Board(bno=20, title=제목...20, content=내용...20, writer=user0)
    }  // testSelect 메서드 종료

    @Test
    public void testUpdate(){

        Long bno = 20L; // 20번 게시물을 가져와서 수정후 테스트 종료

        Optional<Board> result = boardRepository.findById(bno); // bno 를 찾아서 result에 넣는다.

        Board board = result.orElseThrow(); // 가져온 값이 있으면 board 타입에 객체에 넣는다.

        board.change("수정테스트 제목2", "수정테스트 내용2"); // 제목과 내용만 수정할 수 있는 메서드

        boardRepository.save(board); // .save 메서드는 pk값이 없으면 insert, pk 있으면 update 함.

    }

    @Test
    public void testDelete(){

        Long bno = 1L;

        boardRepository.deleteById(bno);
        //             .deleteById(bno) -> delecte from board where bno = bno

    }

    @Test
    public void testPaging(){
        // .findAll() 는 모든 리스트를 출력하는 메서드 select * from board ;
        // 전체 리스트에 페이징과 정렬 기법도 추가 해보자.

        Pageable pageable = PageRequest.of(0,10, Sort.by("bno").descending());
        //                           시작번호,페이지당 데이터 개수
        //                                       번호를 기준으로 내림차순 정렬!!!

        Page<Board> result = boardRepository.findAll(pageable);
        // 1장에 종이에 Board 객체를 가지고 있는 결과는 result 에 담긴다.
        // Page 클래스는 다음페이지 존재 여부, 이전페이지 존재 여부, 전체 데이터 개수, 등등.... 계산을 한다.

        log.info("전체 게시물 수 : " + result.getTotalElements());  // 20
        log.info("총 페이지 수 : " + result.getTotalPages());       // 2
        log.info("현재 페이지 번호 : " + result.getNumber());       // 0
        log.info("페이지당 데이터 개수 : " + result.getSize() );     // 10
        log.info("다음페이지 여부 : " + result.hasNext());          // true
        log.info("시작페이지 여부 : " + result.isFirst());         // true

        // 콘솔에 결과를 출력해보자.
        List<Board> boardList = result.getContent(); // 페이징처리된 내용을 가져와라

        boardList.forEach(board -> log.info(board));
        //   forEach는 인덱스를 사용하지 않고 앞에서부터 객체를 리턴함
        //                board -> log.info(board)
        //                      람다식 1개의 명령어가 있을 때 활용

    }

    // 쿼리dsl 테스트 진행
    @Test
    public void testSearch1(){

        // 0번째 페이지(실제 1페이지), 한 페이지당 10개, bno 내림차순
        Pageable pageable = PageRequest.of(0,10, Sort.by("bno").descending());

        // 쿼리DSL로 검색 + 페이징 처리
        Page<Board> result = boardRepository.search1(pageable); //페이징 기법을 사용해서 title = 1 값을 찾아 오나?

        if(result != null && result.hasContent()) {
            log.info("총 페이지 수: " + result.getTotalPages()); // 1
            log.info("총 게시물 수: " + result.getTotalElements()); // 1
            log.info("현재 페이지: " + (result.getNumber() + 1)); // 사용자 기준 페이지 1

            result.getContent().forEach(board -> {
                if(board != null) {
                    log.info("bno: {}, title: {}", board.getBno(), board.getTitle()); // bno: 12, title: 제목...11
                } else {
                    log.warn("조회된 게시물 중 null 존재");
                }
            });
        } else {
            log.warn("조회된 데이터가 없습니다.");
        }

    }

    @Test
    public void testSearchAll(){
        // 프론트에서 t가 선택되면 title, c가 선택되면 content, w가 선택되면 writer가 조건으로 제시됨

        String[] types = {"t", "w"};  // 검색 조건

        String keyword = "10";  // 검색 단어

        Pageable pageable = PageRequest.of(0,10, Sort.by("bno").descending());

        Page<Board> result = boardRepository.searchAll(types, keyword, pageable);

        log.info("전체 게시물 수 : " + result.getTotalElements());  // 1
        log.info("총 페이지 수 : " + result.getTotalPages());       // 1
        log.info("현재 페이지 번호 : " + result.getNumber());       // 0
        log.info("페이지당 데이터 개수 : " + result.getSize() );     // 10
        log.info("다음페이지 여부 : " + result.hasNext());          // false
        log.info("시작페이지 여부 : " + result.isFirst());         // true

        result.getContent().forEach(board -> log.info(board));

    }

    @Test
    public void testSearchReplyCount(){

        String[] types= {"t","c","w"};  // 제목, 내용, 작성자
        String keyword = "1";           // 제목이나 내용이나 작성자에 1값을 찾는다.

        Pageable pageable = PageRequest.of(0,10, Sort.by("bno").descending());

        Page<BoardListReplyCountDTO> result = boardRepository.searchWithReplyCount(types, keyword, pageable);

        log.info("전체 게시물 수 : " + result.getTotalElements());  // 10
        log.info("총 페이지 수 : " + result.getTotalPages());       // 1
        log.info("현재 페이지 번호 : " + result.getNumber());       // 0
        log.info("페이지당 데이터 개수 : " + result.getSize() );     // 10
        log.info("다음페이지 여부 : " + result.hasNext());          // false
        log.info("시작페이지 여부 : " + result.isFirst());         // true

        result.getContent().forEach(board -> log.info(board));
        // BoardListReplyCountDTO(bno=100, title=제목...100(수정테스트), writer=user0, regDate=2025-07-22T11:11:46.002548, replyCount=2)
    }

    //board 이미지처리 테스트
    @Test
    public void testInsertWithImage(){

        Board board = Board.builder()
                .title("이미지 테스트")
                .content("첨부파일테스트")
                .writer("tester")
                .build();

        // 첨부파일 더미데이터 string 처리
        for (int i=0 ; i < 3 ; i++ ){
            // 첨부파일 3개
            board.addImage(UUID.randomUUID().toString(), "file"+i+".jpg");
            // import java.util.UUID;   UUIDfile0.jpg UUIDfile1.jpg UUIDfile2.jpg

        }
        boardRepository.save(board);

    }

    @Test // 게시물 읽기 + 이미지
    @Transactional // import jakarta.transaction.Transactional;
    public void testReadWithImage(){

        Optional<Board> result = boardRepository.findById(10L);
        // board 테이블에 10번 게시물을 가져와라

        Board board = result.orElseThrow(); // 예외가 없으면 board 객체에 담는다.

        log.info(board); // 게시물 객체
        log.info("-----------------------");
        log.info(board.getImageSet()); // 첨부파일 객체

    }

    @Test
    public void testReadWithImagesEntityGraph(){

        Optional<Board> result = boardRepository.findByIdWithImage(10L);
        //                                       repository에 만든 JQPL 활용  @EntityGraph

        Board board = result.orElseThrow();

        log.info(board);
        log.info("-----------------------");

        for (BoardImage boardImage : board.getImageSet()) {
            log.info(boardImage);
        }

    }

    @Transactional
    @Commit // 두 테이블이 결과가 둘다 ok(ture) 처리되면 영구 저장
    @Test
    public void testModifyImages(){

        Optional<Board> result = boardRepository.findByIdWithImage(10L);
        Board board = result.orElseThrow();

        board.clearImage(); // board 테이블에 연결된 Image 테이블을 전체 삭제

        for(int i=0 ; i < 2 ; i++ ){
            // 전에는 3개의 첨부지만 2로 수정 하려 함
            board.addImage(UUID.randomUUID().toString(), "updatefile"+i+".jpg");

        }
        boardRepository.save(board);

    }

    @Test
    @Transactional
    @Commit
    public void testRemoveAll(){
        // 10번 게시물을 삭제하면 댓글과 첨부파일이 모두 삭제되어야 함!!!

        Long bno = 10L;

        replyRepository.deleteByBoard_Bno(bno);  // 자식부터 삭제
        boardRepository.deleteById(bno);        // 부모가 삭제

    }

    @Test
    public void testInsertAll(){
        // 게시글과 첨부파일 더미데이터 추가용

        for(int i=1 ; i <= 20 ; i++ ){

            Board board = Board.builder()
                    .title("테스트 제목" + i)
                    .content("테스트 내용"+i)
                    .writer("writer"+i)
                    .build();

            for (int j=0 ; j < 3 ; j++){

                if(i % 5 == 0){
                    continue;  // 5의 배수 게시물에는 첨부파일이 없다.!!!!
                }
                board.addImage(UUID.randomUUID().toString(), i+"file"+j+".jpg");
            } // 첨부파일 더미데이터 for문 종료
            boardRepository.save(board);
        } // 게시물 더미데이터 for 종료
    } // testInsertAll 메서드 종료



    @Test // N+1 오류 발생 테스트
    @Transactional
    public void testSearchImageReplyCount(){
        // 리스트 페이지에서 댓글 수와 게시물목록 이미지가 처리되는 부분

        Pageable pageable = PageRequest.of(1,10, Sort.by("bno").descending());
        boardRepository.searchWithAll(null, null, pageable);
        //                           타입  키워드

    }

} // 클래스 종료