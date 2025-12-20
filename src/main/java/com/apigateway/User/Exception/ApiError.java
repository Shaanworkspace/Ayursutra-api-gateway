package com.apigateway.User.Exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiError {
	private int status;
	private String message;
	private LocalDateTime timestamp;
}
