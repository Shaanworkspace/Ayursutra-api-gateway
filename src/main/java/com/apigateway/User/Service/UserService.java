package com.apigateway.User.Service;


import com.apigateway.User.DTO.Request.LoginRequest;
import com.apigateway.User.DTO.Request.RegisterRequest;
import com.apigateway.User.DTO.Response.UserResponse;
import com.apigateway.User.Entity.User;
import com.apigateway.User.Enum.Role;
import com.apigateway.User.Repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

	private final UserRepository userRepository;

	public UserResponse registerUser(RegisterRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new IllegalArgumentException("User already exists with email " + request.getEmail());
		}

		Role role = Role.valueOf(request.getRole().toUpperCase());

		User user = User.builder()
				.auth0Id(request.getAuth0Id())
				.email(request.getEmail())
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.phone(request.getPhone())
				.roles(List.of(role))
				.build();

		User saved = userRepository.save(user);

		return mapToResponse(saved);
	}

	public UserResponse getUserByAuth0Id(String auth0Id) {
		User user = userRepository.findByAuth0Id(auth0Id)
				.orElseThrow(() -> new RuntimeException("User not found with auth0Id: " + auth0Id));
		return mapToResponse(user);
	}

	public UserResponse mapToResponse(User user) {
		return UserResponse.builder()
				.id(user.getId())
				.auth0Id(user.getAuth0Id())
				.email(user.getEmail())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.phone(user.getPhone())
				.roles(user.getRoles())
				.build();
	}
}