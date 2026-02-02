package com.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class DeduplicateCorsHeadersFilter implements GlobalFilter, Ordered {

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
		return chain.filter(exchange).then(Mono.fromRunnable(() -> {
			HttpHeaders headers = exchange.getResponse().getHeaders();

			for (String headerName : CORS_HEADERS) {
				List<String> values = headers.get(headerName);
				if (values != null && values.size() > 1) {
					String firstValue = values.getFirst();
					headers.set(headerName, firstValue);
				}
			}
		}));
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
}