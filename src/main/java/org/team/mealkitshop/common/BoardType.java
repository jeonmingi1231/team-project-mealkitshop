package org.team.mealkitshop.common;


public enum BoardType {

    NOTICE("공지사항"),
    QNA("질의응답"),
    EVENT_BOARD("이벤트 게시판"),
    REVIEW_BOARD("후기 게시판"),
    TIP_BOARD("팁 게시판");

    private final String label;

    BoardType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

