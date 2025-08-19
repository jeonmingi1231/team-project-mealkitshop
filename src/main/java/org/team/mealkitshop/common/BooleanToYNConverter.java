package org.team.mealkitshop.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true) // autoApply=true 면 모든 Boolean 필드에 자동 적용
public class BooleanToYNConverter implements AttributeConverter<Boolean, String> {
 // 대표 이미지 판단 여부 기존 String 타입으로 적용 시 Y / N 값 이외에 다른 문자열 값이 들어올 수 있으므로
 // Boolean 타입 "true / false" 판단 여부를 "Y / N" 으로 변경하여 DB에 저장 DB 컬럼 필드는 (length = 1) 로 지정
    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        if (Boolean.TRUE.equals(attribute)) {
            return "Y"; // Boolean 타입이 TRUE 값일 경우 "Y"로 반환
        } else {
            return "N"; // Boolean 타입이 TRUE 값일 경우 "N"로 반환
        }
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        return "Y".equals(dbData);
        //private boolean repimgYn; //대표 이미지 여부 에 대해서
        // Y (true)값일 경우 db로 반환 : (대표이미지 판단 여부 관련 (repimgYn) colunm에
        // 대표 이미지 사용시 DB 값 "Y"로 찍힘)
    }
}
