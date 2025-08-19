package org.team.mealkitshop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.board.BoardReaction;
import org.team.mealkitshop.dto.board.BoardReactionDTO;
import org.team.mealkitshop.repository.BoardReactionRepository;
import org.team.mealkitshop.repository.BoardRepository;

@Service
@RequiredArgsConstructor
public class BoardReactionServiceImpl implements BoardReactionService {

    private final BoardReactionRepository boardReactionRepository;
    private final BoardRepository boardRepository;

    @Override
    public void reactToBoard(BoardReactionDTO reactionDTO){

        // 게시글 존재 여부 확인
        Board board = boardRepository.findById(reactionDTO.getBoardId()).orElseThrow(()
        -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        // 리액션 엔티티 생성
        BoardReaction reaction = BoardReaction.builder()
                .boardId(reactionDTO.getBoardId())
                .userId(reactionDTO.getUserId())
                .reaction(reactionDTO.getReaction())
                .build();

        // 저장
        boardReactionRepository.save(reaction);
    }

}
