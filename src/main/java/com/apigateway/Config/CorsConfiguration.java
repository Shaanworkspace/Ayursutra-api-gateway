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

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public WebFilter corsWebFilter() {
		return (ServerWebExchange exchange, WebFilterChain chain) -> {
			ServerHttpRequest request = exchange.getRequest();
			ServerHttpResponse response = exchange.getResponse();
			HttpHeaders headers = response.getHeaders();

			String origin = request.getHeaders().getOrigin();

			// Add CORS headers for ALL requests (not just preflight)
			if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
				headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
				headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
						"GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
				headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
						"Authorization, Content-Type, X-Requested-With, Accept, Origin, Cache-Control");
				headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
				headers.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
						"Authorization, Content-Type");
				headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
			}

			// Handle preflight OPTIONS request
			if (request.getMethod() == HttpMethod.OPTIONS) {
				response.setStatusCode(HttpStatus.OK);
				return Mono.empty();
			}

			// Continue filter chain and add headers to response
			return chain.filter(exchange)
					.then(Mono.fromRunnable(() -> {
						// Ensure headers are set on response
						if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
							HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
							if (!responseHeaders.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)) {
								responseHeaders.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
								responseHeaders.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
							}
						}
					}));
		};
	}
}