package com.encipherhealth.chatapp.exception;

import com.encipherhealth.chatapp.config.StatusCode;
import com.encipherhealth.chatapp.dto.StatusResponse;
import com.encipherhealth.chatapp.util.Messages;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler(DuplicateChatRoomException.class)
    public ResponseEntity<StatusResponse> handleDuplicate(DuplicateChatRoomException ex, HttpServletRequest request)
    {
        log.error("Duplicate chat room | path={} | error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(StatusCode.CONFLICT).body(StatusResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ChatRoomNotFoundException.class)
    public ResponseEntity<StatusResponse> handleNotFound(ChatRoomNotFoundException ex, HttpServletRequest request)
    {
        log.error("Chat room not found | path={} | error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(StatusCode.NOT_FOUND).body(StatusResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ParticipantNotJoinedException.class)
    public ResponseEntity<StatusResponse> handleNotJoined(ParticipantNotJoinedException ex, HttpServletRequest request)
    {
        log.error("Participant not joined | path={} | error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(StatusCode.UNPROCESSABLE_ENTITY).body(StatusResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<StatusResponse> handleBusiness(CustomException ex, HttpServletRequest request)
    {
        log.error("Business exception | path={} | error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(StatusCode.UNPROCESSABLE_ENTITY).body(StatusResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StatusResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request)
    {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");
        log.error("Validation failed | path={} | error={}", request.getRequestURI(), message);
        return ResponseEntity.status(StatusCode.BAD_REQUEST).body(StatusResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StatusResponse> handleSystem(Exception ex, HttpServletRequest request)
    {
        log.error("Unexpected error | path={}", request.getRequestURI(), ex);
        return ResponseEntity.status(StatusCode.EXCEPTION).body(StatusResponse.error(Messages.FAILED));
    }
}
