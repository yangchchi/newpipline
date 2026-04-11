package com.zhinvest.map.exception;

import com.zhinvest.map.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        HttpStatus http = switch (e.getCode()) {
            case 404 -> HttpStatus.NOT_FOUND;
            case 400 -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(http).body(new ApiResponse<>(e.getCode(), e.getMessage(), null));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class
    })
    public ApiResponse<Void> handleBadRequest(Exception e) {
        String message = "请求参数无效";
        if (e instanceof MethodArgumentNotValidException m) {
            FieldError fe = m.getBindingResult().getFieldError();
            if (fe != null && fe.getDefaultMessage() != null) {
                message = fe.getDefaultMessage();
            }
        } else if (e instanceof MissingServletRequestParameterException m) {
            message = "缺少参数: " + m.getParameterName();
        } else if (e instanceof ConstraintViolationException c) {
            message = c.getConstraintViolations().iterator().next().getMessage();
        } else if (e instanceof HandlerMethodValidationException h) {
            var results = h.getParameterValidationResults();
            if (!results.isEmpty()) {
                var res = results.getFirst().getResolvableErrors().getFirst();
                message = res.getDefaultMessage();
            }
        }
        return new ApiResponse<>(400, message, null);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception e) {
        return new ApiResponse<>(500, "服务器内部错误", null);
    }
}
