package com.apigateway.User.DTO.Response;
import com.apigateway.User.Enum.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponse {
	private Long id;
	private String auth0Id;
	private String email;
	private String firstName;
	private String lastName;
	private String phone;
	private List<Role> roles;
}