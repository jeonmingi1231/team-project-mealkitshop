package org.team.mealkitshop.service.board;

import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.dto.board.*;

import java.util.List;
import java.util.stream.Collectors;

public interface BoardService {
    // 시그니쳐만 필요 -> Impl 구현클래스 -> 실행문을 만든다.

    Long register(BoardDTO boardDTO); // 프론트에서 폼에있는 내용이 dto로 들어온다.
    //리턴은 bno가 된다.

    BoardDTO readOne(Long bno); // 프론트에서 bno가 넘어오면 객체가 리턴된다.

    void modify(BoardDTO boardDTO); // 프론트에서 dto가 넘어오면 수정 작업

    void remove(Long bno); // 프론트에서 bno가 넘어오면 삭제 작업 진행

    PageResponseDTO<BoardDTO> list(PageRequestDTO pageRequestDTO);
    // 페이징 처리에 대한 요청을 리스트로 처리하고 결과를 응답으로 보내는 메서드!!!

    // p547 페이징 + 정렬 + 댓글 수 추가
    PageResponseDTO<BoardListReplyCountDTO> listWithCommentCount(PageRequestDTO pageRequestDTO);

    default Board dtoTOEntity(BoardDTO boardDTO) { // 조장이 만드는 실행문!!
        // 인터페이스에 default 접근 제한자를 설정하면 강제로 구현클래스 중지
        // BoardServiceImpl 클래스에서 메서드를 오버라이딩 하지 않음.

        // dto로 받아서 엔티티로 리턴
        Board board = Board.builder()
                .bno(boardDTO.getBno())
                .title(boardDTO.getTitle())
                .content(boardDTO.getContent())
                .writer(boardDTO.getWriter())
                .build();

        if(boardDTO.getFileNames() != null) {
            // 프론트에서 넘어온 dto가 첨부파일명이 있으면!!!
            boardDTO.getFileNames().forEach(fileName -> {
                String[] arr = fileName.split("_"); // UUID_파일명.JPG
                board.addImage(arr[0], arr[1]);
                //             uuid    파일명.jpg
            }); // 파일첨부 람다식 종료
        } // 파일첨부 if문 종료
        return board;
    } // dtoToEntity 메서드 종료

    default BoardDTO entityTODTO(Board board) {
        // 엔티티로 받아서 dto로 나간다
        // db로 받아서 프론트로 나간다.

        BoardDTO boardDTO = BoardDTO.builder()
                .bno(board.getBno())
                .title(board.getTitle())
                .content(board.getContent())
                .writer(board.getWriter())
                .build();

        // 파일 처리용
        List<String> fileNames = board.getImageSet()
                .stream()
                .sorted()
                .map(boardImage ->
                        boardImage.getFileId()+"_"
                                +boardImage.getFileName())
                .collect(Collectors.toList());
        // db에 있는 uuid + 파일명 -> fileNames로 만듬

        boardDTO.setFileNames(fileNames);
        return boardDTO;

    }

    // 모든 리스트를 가져오는 메서드 ( 게시물리스트, 댓글수 , 첨부파일목록, 페이징처리, 정렬, 검색처리)
    PageResponseDTO<BoardListAllDTO> listWithAll(PageRequestDTO pageRequestDTO);

}
