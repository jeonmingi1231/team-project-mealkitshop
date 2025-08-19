package org.team.mealkitshop.domain.board;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.team.mealkitshop.common.BaseTimeEntity;
import java.util.HashSet;
import java.util.Set;

@Entity // 데이터베이스 테이블 관련 객체
@Table(name = "board")
@Getter
@Builder // 빌더 패턴 세터 대신 활용
@AllArgsConstructor // 모든 필드값으로 생성자 만듬
@NoArgsConstructor // 기본생성자
@ToString(exclude = "ImageSet") // (exclude = "ImageSet") 추가
public class Board extends BaseTimeEntity { //  extends BaseEntity (날짜 관련된 jpa 연결)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동번호 생성용
    private Long bno;                   // 게시글 번호

    @Column(length = 30, nullable = false)
    private String title;               // 제목

    @Column(length = 1000, nullable = false)
    private String content;             // 내용

    @Column(length = 15, nullable = false)
    private String writer;              // 작성자

    @Column(name = "secret_board")
    private boolean secretBoard;        // 비밀글 여부

    @Column(name = "secret_password")
    private String secretPassword;      // 비밀글에서 사용할 비밀번호

    public void change(String title, String content){
        // 제목과 내용만 수정하는 메서드 (세터 대체용)
        this.title = title;
        this.content = content;
    }

    @Builder.Default
    @OneToMany(mappedBy = "board", // BoardImage엔티티의 board 변수
            cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    // 영속성을 all 모든 관여        지연로딩 ( 연관된 테이블을 필요시만 참조)
    //                                                   orphanRemoval = true 실제로 삭제용
    // https://choiblack.tistory.com/48                부모가 사라진 고아 자식 객체를 삭제한다.!!!
    @BatchSize(size = 20) // N+1 문제 해결용 코드
    private Set<BoardImage> ImageSet = new HashSet<BoardImage>();
    // Set은 구슬(로또) 주머니 같은 객체 같은 객체는 1개만 보관
    // 인덱스가 없다. 들어가는 순서와 나오는 순서가 다름

    // 이미지 추가용 메서드
    public void addImage(String fileId, String fileName){

        BoardImage boardImage = BoardImage.builder()
                .fileId(fileId)     // 파일명 랜덤 처리
                .fileName(fileName) // 진짜 파일명
                .board(this)        // 연결된 게시물 정보
                .ord(ImageSet.size()) // 순서배정
                .build();
        ImageSet.add(boardImage); // 구슬 주머니에 이미지를 담는다.

    }

    public void clearImage(){

        ImageSet.forEach(boardImage -> boardImage.changeBoard(null));

        this.ImageSet.clear();

    }

}
