package com.apigateway.User.advices;


import com.apigateway.User.Exception.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException e){
		ApiError apiError = ApiError.builder()
				.message(e.getMessage())
				.status(HttpStatus.BAD_REQUEST.value())
				.timestamp(LocalDateTime.now())
				.build();
		return new ResponseEntity<>(apiError,HttpStatus.BAD_REQUEST);
	}
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiError> handleRuntimeException(RuntimeException e){
		ApiError apiError = ApiError.builder()
				.message(e.getMessage())
				.status(HttpStatus.BAD_REQUEST.value())
				.timestamp(LocalDateTime.now())
				.build();
		return new ResponseEntity<>(apiError,HttpStatus.BAD_REQUEST);
	}
}
