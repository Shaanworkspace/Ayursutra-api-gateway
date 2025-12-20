package com.apigateway.User.DTO.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
	private String auth0Id;
	private String email;
	private String firstName;
	private String lastName;
	private String phone;
	private String role;
}