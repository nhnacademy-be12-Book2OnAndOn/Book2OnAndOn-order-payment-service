package com.nhnacademy.Book2OnAndOn_order_payment_service.order.dto.order;

import com.nhnacademy.Book2OnAndOn_order_payment_service.exception.OrderVerificationException;
import com.nhnacademy.Book2OnAndOn_order_payment_service.order.client.dto.BookOrderResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record BookOrderContext(Map<Long, BookOrderResponse> bookMap) {
    public static BookOrderContext from(List<BookOrderResponse> list){
        return new BookOrderContext(
                list.stream().collect(Collectors.toMap(
                        BookOrderResponse::getBookId,
                        Function.identity()
                ))
        );
    }

    public BookOrderResponse get(Long bookId){
        BookOrderResponse resp = bookMap.get(bookId);
        if(resp == null){
            throw  new OrderVerificationException("도서 정보 불일치");
        }

        return resp;
    }
}
