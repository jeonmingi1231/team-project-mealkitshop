package org.team.mealkitshop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.dto.board.*;
import org.team.mealkitshop.repository.BoardRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service // 스프링에게 서비스 계층임을 알린다.
@Log4j2
@RequiredArgsConstructor  // 필드 값을 보고 생성자를 만든다. final필드나 @NonNull이 붙은 필드용
@Transactional // commit용 (여러개의 테이블이 조합될때 해결 역할 )
public class BoardServiceImpl implements BoardService {

    private final ModelMapper modelMapper;  // 엔티티와 dto를 변환
    private final BoardRepository boardRepository; // jpa용 클래스 (CURD, 페이징, 정렬, 다중검색)

    @Override
    public Long register(BoardDTO boardDTO) {  // 조원이 실행코드를 만든다.
        // 폼에서 넘어온 DTO가 데이터베이스에 기록되어야 함.

        // p642 쪽 제거 Board board = modelMapper.map(boardDTO, Board.class); // 엔티티가 dto로 변환
        // p642 쪽 추가
        Board board = dtoTOEntity(boardDTO); // dto를 엔티티로 변환 시켜줌 custom

        Long bno = boardRepository.save(board).getBno();
        //                  insert into board ~~~~ -> bno를 받는다.

        return bno; // 프론트에 게시물 저장 후 번호가 전달 된다.
    }

    @Override
    public BoardDTO readOne(Long bno) {

        // p644변경     Optional<Board> result = boardRepository.findById(bno);
        Optional<Board> result = boardRepository.findByIdWithImage(bno); // 첨부파일추가됨
        // select * from board where bno = bno
        // Optional null이 나와도 예외처리 하지 않음

        Board board = result.orElseThrow(); // 정상값이 나오면 엔티로 받는다.

        // p644 제거 BoardDTO boardDTO = modelMapper.map(board, BoardDTO.class);
        // 모델 메퍼를 이용해서 엔티티로 나온 board를 dto로 변환한다.
        BoardDTO boardDTO = entityTODTO(board);


        return boardDTO;  // 프론트에 dto로 보낸다.
    }

    @Override
    public void modify(BoardDTO boardDTO) {

        Optional<Board> result = boardRepository.findById(boardDTO.getBno());
        // select * from board where bno = bno -> 엔티티로 나옴

        Board board = result.orElseThrow(); // 성공시(null이 아닐때) 결과를 엔티티로 저장
        board.change(boardDTO.getTitle(), boardDTO.getContent()); // 제목과 내용이 수정

        // p645 첨부파일 수정용 (기존에 첨부파일을 삭제하고 -> 다시 등록 알고리즘)
        board.clearImage(); // 첨부파일 삭제

        if(boardDTO.getFileNames() != null) {
            // 프론트에 있는 첨부파일이 null이 아님
            for (String fileName : boardDTO.getFileNames()) {
                String[] arr = fileName.split("_");
                board.addImage(arr[0], arr[1]);
                // 프론트에서 온 파일명 uuid와 파일명을 board 엔티티에 넣는다.
            }
        } // 파일처리 if문 종료

        boardRepository.save(board) ; // 데이터베이스에 pk가 있으면 update, 없으면 insert

    }

    @Override
    public void remove(Long bno) {
        boardRepository.deleteById(bno);
        // delete from board where bno = bno
    }

    @Override  // 리스트 페이지 요청에 온 값을 응답을 보낸다 (페이징 처리)
    public PageResponseDTO<BoardDTO> list(PageRequestDTO pageRequestDTO) {
        // PageRequestDTO에서 넘어온 값을 처리하고 PageResponseDTO로 보내야 한다.

        String[] types = pageRequestDTO.getTypes(); //프론트에 넘어온 type t,c,w 처리
        String keyword = pageRequestDTO.getKeyword(); // 프론트에서 넘어온 검색단어 처리
        Pageable pageable = pageRequestDTO.getPageable("bno");  //프론트에서 넘어온 bno를 이용한 정렬 처리용
        //  return PageRequest.of(this.page-1, this.size, Sort.by(props).descending());

        // Page<Board> -> List<BoardDTO> 변환하고 리턴 되어야 한다.
        Page<Board> result = boardRepository.searchAll(types, keyword, pageable);
        // 테스트에서 해봤던 코드

        List<BoardDTO> dtoList = result.getContent().stream() // stream() 바이트가 전달되는 기법
                .map(board -> modelMapper.map(board,BoardDTO.class)) // 모델메퍼로 엔티티가 dto 변환
                .collect(Collectors.toList()); //Collectors.toList() 리스트로 변환
        // 엔티티를 dto로 변환하는 코드

        return PageResponseDTO.<BoardDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .total((int)result.getTotalElements())
                .build(); // 빌더패턴을 이용해서 리턴할 때 사용함.
    }

    @Override
    public PageResponseDTO<BoardListReplyCountDTO> listWithCommentCount(PageRequestDTO pageRequestDTO) {

        String[] types = pageRequestDTO.getTypes();
        String keyword = pageRequestDTO.getKeyword();
        Pageable pageable = pageRequestDTO.getPageable("bno");

        Page<BoardListReplyCountDTO> result = boardRepository.
                searchWithReplyCount(types, keyword, pageable);

        return PageResponseDTO.<BoardListReplyCountDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(result.getContent())
                .total((int)result.getTotalElements())
                .build();
        // 리스트 페이지에 페이징처리, 정렬, 게시판의리스트, 댓글의 개수가 리턴됨!
    }

    @Override
    public PageResponseDTO<BoardListAllDTO> listWithAll(PageRequestDTO pageRequestDTO) {
        // 검색처리, 페이징, 게시판리스트, 댓글, 첨부파일 등....

        String[] types = pageRequestDTO.getTypes();
        String keyword = pageRequestDTO.getKeyword();
        Pageable pageable = pageRequestDTO.getPageable("bno");
        Page<BoardListAllDTO> result = boardRepository.searchWithAll(types, keyword, pageable);

        return PageResponseDTO.<BoardListAllDTO>withAll().pageRequestDTO(pageRequestDTO)
                .dtoList(result.getContent())
                .total((int)result.getTotalElements())
                .build();
    }

}