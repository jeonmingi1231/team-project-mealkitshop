package org.team.mealkitshop.domain.board;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "board_Image")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "board") // board테이블 제외
public class BoardImage implements Comparable<BoardImage> {
    //                             @OneToMany처리에 순번에 맞게 정렬하기 위함
    // changeBoard()를 이용해서 Board 객체를 나중에 지정할 수 있게
    // Board 엔티티 삭제시 BoardAttachment 객체의 참조도 변경

    @Id
    private String fileId;            // 첨부파일 ID

    private String fileName;        // 서버에 저장된 파일명

    private String oriFileName;     // 사용자가 업로드한 원본 파일명

    private String fileUrl;         // 파일 경로

    private String repImgYn;        // 대표 이미지 여부 Y/N

    private String contentType;     // 확장자 MIME 타입 예) "image/png"

    private long size;              // 파일 크기

    private int ord;                // 파일 순번

    @ManyToOne  // fk로 선언 됨!!!
    private Board board ; // 연습용으로 @ManyToOne

    @Override // 재정의
    public int compareTo(BoardImage other) {
        return this.ord - other.ord;  // 실행 순번용
    }

    public void changeBoard(Board board) {
        this.board = board;  // board 엔티티 변경시 같이 변경용
    }

}
