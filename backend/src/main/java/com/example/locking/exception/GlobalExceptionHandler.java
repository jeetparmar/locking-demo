package com.example.locking.exception;

import com.example.locking.dto.Dtos.ApiError;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private ResponseEntity<ApiError> body(HttpStatus s, String code, String msg) {
		return ResponseEntity.status(s).body(new ApiError(s.value(), code, msg));
	}

	// Covers ObjectOptimisticLockingFailureException too (it is a subclass).
	@ExceptionHandler(OptimisticLockingFailureException.class)
	ResponseEntity<ApiError> optimistic(OptimisticLockingFailureException e) {
		return body(HttpStatus.CONFLICT, "VERSION_CONFLICT", "Seat was modified by someone else. Reload and retry.");
	}

	@ExceptionHandler(PessimisticLockingFailureException.class)
	ResponseEntity<ApiError> pessimistic(PessimisticLockingFailureException e) {
		return body(HttpStatus.LOCKED, "LOCK_TIMEOUT", "Could not acquire the row lock in time. Try again shortly.");
	}

	@ExceptionHandler(SeatAlreadyBookedException.class)
	ResponseEntity<ApiError> booked(SeatAlreadyBookedException e) {
		return body(HttpStatus.CONFLICT, "SEAT_ALREADY_BOOKED", e.getMessage());
	}

	@ExceptionHandler(SeatNotFoundException.class)
	ResponseEntity<ApiError> notFound(SeatNotFoundException e) {
		return body(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e) {
		String msg = e.getBindingResult().getFieldErrors().stream().map(f -> f.getField() + " " + f.getDefaultMessage())
				.reduce((a, b) -> a + "; " + b).orElse("Invalid request");
		return body(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", msg);
	}
}
