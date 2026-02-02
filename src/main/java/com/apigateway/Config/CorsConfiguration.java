package com.apigateway.Config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfiguration {

	private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
			"https://ayursutra-frontend.netlify.app",
			"http://localhost:5173",
			"http://localhost:3000"
	);

	private static final String ALLOWED_METHODS = "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD";
	private static final String ALLOWED_HEADERS = "Authorization, Content-Type, X-Requested-With, Accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers, Cache-Control";
	private static final String EXPOSED_HEADERS = "Authorization, Content-Type";
	private static final String MAX_AGE = "3600";

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public WebFilter corsWebFilter() {
		return (ServerWebExchange exchange, WebFilterChain chain) -> {
			ServerHttpRequest request = exchange.getRequest();

			if (CorsUtils.isCorsRequest(request)) {
				ServerHttpResponse response = exchange.getResponse();
				HttpHeaders headers = response.getHeaders();

				String origin = request.getHeaders().getOrigin();

				// Check if origin is allowed
				if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
					// Remove any existing CORS headers first (prevent duplicates)
					headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
					headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
					headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
					headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
					headers.remove(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
					headers.remove(HttpHeaders.ACCESS_CONTROL_MAX_AGE);

					// Set CORS headers
					headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
					headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
					headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, ALLOWED_HEADERS);
					headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
					headers.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSED_HEADERS);
					headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, MAX_AGE);
				}

				// Handle preflight request
				if (request.getMethod() == HttpMethod.OPTIONS) {
					response.setStatusCode(HttpStatus.OK);
					return Mono.empty();
				}
			}

			return chain.filter(exchange);
		};
	}
}