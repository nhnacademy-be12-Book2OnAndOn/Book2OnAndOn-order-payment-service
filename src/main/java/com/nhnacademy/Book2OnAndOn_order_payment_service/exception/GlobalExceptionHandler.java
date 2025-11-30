package com.nhnacademy.Book2OnAndOn_order_payment_service.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e){
        return ResponseEntity.ok(toErrorResponse(e));
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateException(DuplicateException e){
        return ResponseEntity.ok(toErrorResponse(e));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e){
        log.warn("Toss Payments API 승인 실패");
        return ResponseEntity.ok(toErrorResponse(e));
    }

    private ErrorResponse toErrorResponse(RuntimeException e){
        if(e instanceof NotFoundException){
            return new ErrorResponse(HttpStatus.NOT_FOUND.name(), e.getMessage(), HttpStatus.NOT_FOUND.value());
        }else if(e instanceof DuplicateException){
            return new ErrorResponse(HttpStatus.CONFLICT.name(), e.getMessage(), HttpStatus.CONFLICT.value());
        }else if(e instanceof FeignException fe){
            //TODO ErrorDecorder 사용, Feign전용 ErrorDecoder를 사용 전체 메세지를 담고있음
            ErrorResponse err = null;
            try {
                err = objectMapper.readValue(fe.getMessage(), ErrorResponse.class);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
            return new ErrorResponse(err.code(), err.message(), fe.status());
        }
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.name(), e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
