package com.jaramgroupware.auth.exceptions;

import com.google.firebase.FirebaseException;
import com.jaramgroupware.auth.dto.general.controllerDto.ExceptionMessageDto;
import com.jaramgroupware.auth.exceptions.firebase.FirebaseApiException;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ HttpMediaTypeNotSupportedException.class })
    protected ResponseEntity<ExceptionMessageDto> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException exception, WebRequest request) {
        log.info("Path : {} -> Throw HttpMediaTypeNotSupportedException.",request.getContextPath());
        return ResponseEntity.badRequest().body(ExceptionMessageDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .title("처리할 수 없는 MediaType입니다.")
                .detail("해당 API에서 처리 불가능한 MediaType 입니다. API 문서를 확인하시고 올바른 MediaType으로 다시 요청해주세요")
                .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionMessageDto> processHttpMessageNotReadableException(HttpMessageNotReadableException exception, WebRequest request) {
        log.info("Path : {} -> Throw HttpMessageNotReadableException.",request.getContextPath());
        return ResponseEntity.badRequest().body(ExceptionMessageDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .title("잘못된 입력입니다.")
                .detail("입력받은 Request를 올바르게 변환할 수 없습니다. 입력 형식을 다시 확인해주세요.")
                .build());
    }

    @ExceptionHandler({ HttpRequestMethodNotSupportedException.class })
    protected ResponseEntity<ExceptionMessageDto> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception, WebRequest request) {
        log.info("Path : {} -> Throw HttpRequestMethodNotSupportedException.",request.getContextPath());
        return ResponseEntity.badRequest().body(ExceptionMessageDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .title("잘못된 Method 입니다.")
                .detail("해당 API에서 처리 불가능한 Method 입니다. API 문서를 확인하고 올바른 Method로 다시 요청해주세요")
                .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionMessageDto> processMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception, WebRequest request) {
        log.info("Path : {} -> Throw MethodArgumentTypeMismatchException.",request.getContextPath());
        return ResponseEntity.badRequest().body(ExceptionMessageDto.builder()
                .status(HttpStatus.BAD_REQUEST)
                .title("잘못된 Type 입니다.")
                .detail("입력 받은 인자 중 타입이 잘못된 인자가 있습니다. API 문서를 확인하고 올바른 타입으로 다시 요청해주세요. ")
                .build());
    }

    @ExceptionHandler({ Exception.class })
    protected ResponseEntity<ExceptionMessageDto> handleServerException(Exception exception, WebRequest request) {
        log.info("Path : {} -> Throw {} / Message {}",request.getContextPath(),exception.getClass().getSimpleName(),exception.getMessage());

        return ResponseEntity.internalServerError().body(ExceptionMessageDto.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .title("서버에 오류가 발생했습니다.")
                .detail("서버에 알 수 없는 오류가 발생했습니다. 잠시후 다시 시도해주세요.")
                .build());
    }

    @ExceptionHandler({ JGWAuthException.class })
    protected ResponseEntity<ExceptionMessageDto> handleJGWAuthException(JGWAuthException exception, WebRequest request) {

        log.info("Path : {} -> Throw JGWAuthException / Message {}",request.getContextPath(),exception.getJgwAuthErrorCode().getTitle());

        return ResponseEntity.status(exception.getJgwAuthErrorCode().getHttpStatus()).body(ExceptionMessageDto.builder()
                .status(exception.getJgwAuthErrorCode().getHttpStatus())
                .title(exception.getJgwAuthErrorCode().getTitle())
                .detail(exception.getDetail())
                .build());
    }

    @ExceptionHandler({ FirebaseApiException.class })
    protected ResponseEntity<ExceptionMessageDto> handleFirebaseApiException(FirebaseApiException exception, WebRequest request) {

        log.info("Path : {} -> Throw FirebaseApiException / Message {}",request.getContextPath(),exception.getFireBaseErrorCode().getTitle());

        return ResponseEntity.status(exception.getFireBaseErrorCode().getHttpStatus()).body(ExceptionMessageDto.builder()
                .status(exception.getFireBaseErrorCode().getHttpStatus())
                .title(exception.getFireBaseErrorCode().getTitle())
                .detail(exception.getFireBaseErrorCode().getDetail())
                .build());
    }
}