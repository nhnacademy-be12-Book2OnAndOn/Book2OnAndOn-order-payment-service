//package com.nhnacademy.Book2OnAndOn_order_payment_service.order.converter;
//
//import com.nhnacademy.Book2OnAndOn_order_payment_service.order.entity.refund.RefundStatus;
//import jakarta.persistence.AttributeConverter;
//import jakarta.persistence.Converter;
//
///**
// * ReturnStatus Enum과 DB의 TINYINT(Integer) 타입 간의 변환을 담당합니다.
// */
//@Converter(autoApply = true)
//public class RefundStatusConverter implements AttributeConverter<RefundStatus, Integer> {
//    // JPA가 엔티티 ↔ DB 간 매핑을 할 때:
//    // Java → DB 저장 전: convertToDatabaseColumn(X attribute) 실행 → Y(Integer)로 변환
//    // DB → Java 로딩 시: convertToEntityAttribute(Y dbData) 실행 → X(ReturnStatus)로 변환
//
//    @Override
//    // 논리값(enum) → 물리값(Integer, TINYINT) 로 변환하는 메서드
//    public Integer convertToDatabaseColumn(RefundStatus attribute) {
//        return attribute == null ? null : attribute.getCode();
//    }
//
//    @Override
//    // 물리값(Integer, TINYINT) → 논리값(enum) 으로 변환하는 메서드
//    public RefundStatus convertToEntityAttribute(Integer dbData) {
//        return dbData == null ? null : RefundStatus.fromCode(dbData);
//    }
//}