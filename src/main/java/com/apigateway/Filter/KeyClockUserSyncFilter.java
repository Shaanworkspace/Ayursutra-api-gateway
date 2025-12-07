package com.apigateway.Filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;

@Slf4j
@Component
public class KeyClockUserSyncFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        // Request details निकालना
        var request = exchange.getRequest();
        var method = request.getMethod();
        URI requestUri = request.getURI();
        String path = request.getPath().toString();
        String traceId = exchange.getRequest().getHeaders().getFirst("traceparent"); // अगर tracing चालू है
        if (traceId == null) traceId = "N/A";

        long startTime = System.currentTimeMillis();

        log.info("\n➡️  [{}] Incoming request: {} {}\n    TraceID: {}\n    From IP: {}",
                Instant.ofEpochMilli(startTime), method, path, traceId,
                request.getRemoteAddress());


        return chain.filter(exchange)
                .doOnSuccess(done -> {
                    long duration = System.currentTimeMillis() - startTime;
                    HttpStatus status = (HttpStatus) exchange.getResponse().getStatusCode();
                    log.info("✅  [{}} Completed {} {} -> {} in {} ms",
                            Instant.ofEpochMilli(System.currentTimeMillis()), method, path, status, duration);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("❌  [{}] Error processing {} {} after {} ms",
                            Instant.ofEpochMilli(System.currentTimeMillis()), method, path, duration, error);
                });
    }
}