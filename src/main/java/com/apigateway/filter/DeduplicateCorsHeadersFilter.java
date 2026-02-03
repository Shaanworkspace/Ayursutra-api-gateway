package com.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class DeduplicateCorsHeadersFilter implements GlobalFilter, Ordered {

	private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
			"https://ayursutra-frontend.netlify.app",
			"http://localhost:5173",
			"http://localhost:3000"
	);

	private static final String[] CORS_HEADERS = {
			HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
			HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
			HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
			HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
			HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
			HttpHeaders.ACCESS_CONTROL_MAX_AGE
	};

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		log.info("GATEWAY RECEIVING: {} {}", exchange.getRequest().getMethod(), exchange.getRequest().getURI());

		String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
		log.info("GATEWAY AUTH HEADER: {}", authHeader != null ? "PRESENT (Starts with " + authHeader.substring(0, 15) + "...)" : "MISSING");

		return chain.filter(exchange).then(Mono.fromRunnable(() -> {
			HttpHeaders headers = exchange.getResponse().getHeaders();
			String origin = exchange.getRequest().getHeaders().getOrigin();

			// Deduplicate CORS headers
			for (String headerName : CORS_HEADERS) {
				List<String> values = headers.get(headerName);
				if (values != null && values.size() > 1) {
					String firstValue = values.get(0);
					headers.set(headerName, firstValue);
				}
			}

			// Ensure CORS headers are present
			if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
				if (!headers.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)) {
					headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
					headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
					headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
							"GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
					headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
							"Authorization, Content-Type, X-Requested-With, Accept, Origin, X-Gateway-Request, Cache-Control");
				}
			}

			log.info("GATEWAY SENDING RESPONSE: {} , with Header: {}", exchange.getResponse().getStatusCode(),headers);
		}));
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
}