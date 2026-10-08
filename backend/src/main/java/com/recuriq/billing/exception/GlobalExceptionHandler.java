package com.recuriq.billing.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.recuriq.billing.dto.error.ErrorInfo;

@RestControllerAdvice
public class GlobalExceptionHandler {

	// 1. Catches Validation (@Valid / Regex) Errors -> Returns HTTP 400
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorInfo> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
		String validationErrors = ex.getBindingResult().getFieldErrors().stream().map(err -> err.getDefaultMessage())
				.collect(Collectors.joining(" | "));

		ErrorInfo errorInfo = ErrorInfo.builder().timestamp(LocalDateTime.now()).status(HttpStatus.BAD_REQUEST.value())
				.error("Validation Failed").message(validationErrors)
				.path(((ServletWebRequest) request).getRequest().getRequestURI()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorInfo);
	}

	// 2. Catches our CustomException -> Preserves the exact HTTP status (e.g., 402
	// Payment Failed)
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorInfo> handleCustomException(CustomException ex, WebRequest request) {
		ErrorInfo errorInfo = ErrorInfo.builder().timestamp(LocalDateTime.now()).status(ex.getStatus().value())
				.error(ex.getStatus().getReasonPhrase()).message(ex.getMessage())
				.path(((ServletWebRequest) request).getRequest().getRequestURI()).build();
		return ResponseEntity.status(ex.getStatus()).body(errorInfo);
	}

	// 3. Catches Security / Bad Password Exceptions -> Returns HTTP 401
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorInfo> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
		ErrorInfo errorInfo = ErrorInfo.builder().timestamp(LocalDateTime.now()).status(HttpStatus.UNAUTHORIZED.value())
				.error("Unauthorized").message("LOGIN FAILED: Incorrect email or password.")
				.path(((ServletWebRequest) request).getRequest().getRequestURI()).build();

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorInfo);
	}

	// 3. Catches Our Custom AuthService "Access Denied" rules -> Returns HTTP 403
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ErrorInfo> handleRuntimeException(RuntimeException ex, WebRequest request) {
		// If it starts with "Access Denied", pass 403
		if (ex.getMessage() != null && ex.getMessage().startsWith("Access Denied")) {
			ErrorInfo errorInfo = ErrorInfo.builder().timestamp(LocalDateTime.now())
					.status(HttpStatus.FORBIDDEN.value()).error("Access Denied").message(ex.getMessage())
					.path(((ServletWebRequest) request).getRequest().getRequestURI()).build();
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorInfo);
		}

		// Generic 400 for anything else
		ErrorInfo errorInfo = ErrorInfo.builder().timestamp(LocalDateTime.now()).status(HttpStatus.BAD_REQUEST.value())
				.error("Bad Request").message(ex.getMessage())
				.path(((ServletWebRequest) request).getRequest().getRequestURI()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorInfo);
	}
}
