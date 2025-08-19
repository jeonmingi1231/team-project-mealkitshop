package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseEntity;

@Entity     // 테이블 관리용 객체
@Getter     // 게터용
@Builder    // 세터 대신 빌더패턴 필수로 @AllArgsConstructor @NoArgsConstructor
@AllArgsConstructor // 모든 필드를 생성자 파라미터로 처리
@NoArgsConstructor  // 기본생성자용
@ToString  // board 제외하고 toString 처리 (객체로 이미 되어 있음) (exclude = "board")
public class Reply extends BaseEntity { // extends BaseEntity 등록일, 수정일 처리용 객체

    @Id // pk로 선언
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동번호생성
    private Long rno; //게시물 번호

    @ManyToOne(fetch = FetchType.LAZY) //지연로딩 천천히하는 로딩
    // 추천! LAZY 로딩에는 no session이라는 예외가 발생한다. -> @Transactional 코드 필수
    // EAGER 로딩은 연결된 모든 테이블에 값을 가져옴 -> db가 힘들어함!
    private Board board; // 게시글 fk처리해야함.    -> board_bno bigint
    // Comment 테이블을 생성하면서 Board에 id값을 확인하여 fk로 선언함

    private String ReplyText;   // 댓글내용

    private String Replyer;     // 댓글 작성자
    // 등록날짜와 수정날짜는 상속받아 처리

    // 세터 대신 변경시 활용되는 메서드 (댓글 수정은 text만 가능)
    public void changeText(String text){
        this.ReplyText = text;
    }

}
