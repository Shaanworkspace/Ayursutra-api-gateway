package com.apigateway.User.Controller;


import com.apigateway.User.DTO.Request.LoginRequest;
import com.apigateway.User.DTO.Request.RegisterRequest;
import com.apigateway.User.DTO.Response.UserResponse;
import com.apigateway.User.Entity.User;
import com.apigateway.User.Repository.UserRepository;
import com.apigateway.User.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;
	private final UserRepository userRepository;

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@RequestBody RegisterRequest request) {
		return userService.registerUser(request);
	}

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse login(@RequestBody LoginRequest request) {
		User u = userRepository.findByEmail(request.getEmail()).orElseThrow(()-> new RuntimeException("User not Found"));
		return userService.mapToResponse(u);
	}


	@GetMapping("/{auth0Id}")
	public UserResponse getUserByAuth0Id(@PathVariable String auth0Id) {
		return userService.getUserByAuth0Id(auth0Id);
	}
}