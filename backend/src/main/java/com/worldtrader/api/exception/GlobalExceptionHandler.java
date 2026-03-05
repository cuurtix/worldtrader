package com.worldtrader.api.exception;

import com.worldtrader.api.market.secure.config.CorrelationIdFilter;
import com.worldtrader.api.market.secure.error.InsufficientFundsError;
import com.worldtrader.api.market.secure.error.InvalidOrderError;
import com.worldtrader.api.market.secure.error.MarketClosedError;
import com.worldtrader.api.market.secure.error.SystemError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ApiError> handleStockNotFound(StockNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientFundsError.class)
    public ResponseEntity<ApiError> handleInsufficient(InsufficientFundsError ex, HttpServletRequest request) {
        return build(HttpStatus.PAYMENT_REQUIRED, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidOrderError.class)
    public ResponseEntity<ApiError> handleInvalid(InvalidOrderError ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MarketClosedError.class)
    public ResponseEntity<ApiError> handleClosed(MarketClosedError ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(SystemError.class)
    public ResponseEntity<ApiError> handleSystem(SystemError ex, HttpServletRequest request) {
        log.error("{{\"correlationId\":\"{}\",\"error\":\"{}\"}}", MDC.get(CorrelationIdFilter.CORRELATION_ID), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        int status = ex.getStatusCode().value();
        String reason = ex.getReason() == null ? ex.getMessage() : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiError(Instant.now(), status, ex.getStatusCode().toString(), reason, request.getRequestURI()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid request", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("{{\"correlationId\":\"{}\",\"error\":\"{}\"}}", MDC.get(CorrelationIdFilter.CORRELATION_ID), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
