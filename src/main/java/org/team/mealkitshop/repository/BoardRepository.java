package org.team.mealkitshop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.repository.Search.BoardSearch;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long>, BoardSearch {
    // extends JpaRepository<엔티티클래스, pk타입>
    // JpaRepository는 jpa에서 미리 만들어 놓은 인터페이스로 C R U D와 페이징처리, 정렬등이 존재한다.

    Page<Board> findByTitleContainingOrderByBnoDesc(String keyword, Pageable pageable);

    //  @Query 쿼리메서드와 병합 -> JPQL
    @Query("select b from Board b where b.title like concat('%',:keyword,'%')")
    Page<Board> findKeyword(String keyword, Pageable pageable);
    // findKeyword 메서드가 실행하면 파라미터로 keyword를 받는다. (제목 검색 단어 where)
    // 쿼리문에 객체가 넘어가야 됨으로 Board가 클래스 명이 되어야 한다.
    // select * from board where title like '%keyword%'

    // 네이티브 쿼리 진짜 쿼리문을 사용하는 기법
    @Query(value = "select now()", nativeQuery = true) // 진짜 쿼리문으로 동작 nativeQuery = true
    String getTime();

    // 이미지 처리하는 select문
    @EntityGraph(attributePaths = {"ImageSet"}) // 지연로딩이지만 같이 로딩해야 하는 값!!
    @Query("select b from Board b where b.bno = :bno")
    Optional<Board> findByIdWithImage(Long bno);

}
