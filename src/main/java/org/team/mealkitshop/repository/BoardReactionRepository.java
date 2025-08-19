package org.team.mealkitshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.team.mealkitshop.domain.board.BoardReaction;

public interface BoardReactionRepository extends JpaRepository<BoardReaction, Long> {

}
