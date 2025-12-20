package com.apigateway.User.DTO.Request;

import lombok.Data;

@Data
public class LoginRequest {
	String email;
	String password;
}
