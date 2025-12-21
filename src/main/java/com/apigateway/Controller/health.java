package com.apigateway.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class health {
	@GetMapping("/health")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("API GATEWAY SERVICE UP");
	}
}
