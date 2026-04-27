package haku.kmm.org.koreatechmajormeeting.global.exception;

import haku.kmm.org.koreatechmajormeeting.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
            .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException e
    ) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : ErrorCode.INVALID_REQUEST.getMessage();

        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
            .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST.getCode(), message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException e
    ) {
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
            .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST.getCode(), ErrorCode.INVALID_REQUEST.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
        MaxUploadSizeExceededException e
    ) {
        return ResponseEntity.status(ErrorCode.STUDENT_CARD_FILE_TOO_LARGE.getStatus())
            .body(ApiResponse.fail(
                ErrorCode.STUDENT_CARD_FILE_TOO_LARGE.getCode(),
                ErrorCode.STUDENT_CARD_FILE_TOO_LARGE.getMessage()
            ));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(
        MultipartException e
    ) {
        String message = e.getMessage();
        if (message != null && message.toLowerCase().contains("size")) {
            return ResponseEntity.status(ErrorCode.STUDENT_CARD_FILE_TOO_LARGE.getStatus())
                .body(ApiResponse.fail(
                    ErrorCode.STUDENT_CARD_FILE_TOO_LARGE.getCode(),
                    ErrorCode.STUDENT_CARD_FILE_TOO_LARGE.getMessage()
                ));
        }
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
            .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST.getCode(), ErrorCode.INVALID_REQUEST.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
            .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
