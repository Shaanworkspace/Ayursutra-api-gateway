package com.apigateway.Filter;

import com.apigateway.Services.UserValidationService;
import com.apigateway.utility.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyClockUserSyncFilter implements WebFilter {

    private final JwtUtil jwtUtil;
    private final UserValidationService userValidationService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if(token==null || !token.startsWith("Barer ")){
            return chain.filter(exchange);
        }

        try{
            String userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.getRole(token);

            if (role == null) {
                log.warn("!!! Unauthorized Role Access");
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            //let's get targeted microservices based on role
            String baseUrl = getBaseUrl(role);
            String existsUrl = baseUrl + "/exists";
            String syncUrl = baseUrl + "/sync";

            // Step 1: Validate if user exists in DB
            Mono<Boolean> userExistsMono = userValidationService.validateUser(token, existsUrl);
            // Step 2: Process based on result
            return userExistsMono.flatMap(userExists -> {
                // Step 2A: If NOT exists → sync user in DB
                if (!userExists) {
                    log.info("Syncing user now as it was now found in DB");

                    // Step 2B: Call microservice to create record & then continue request
                    Mono<Void> syncOperation = userValidationService.syncUser(token, syncUrl);

                    // After sync completes → continue normal routing
                    return syncOperation.then(chain.filter(exchange));
                }

                // Step 3: If user exists → continue normal request
                log.debug("✔ User already synchronized for Role: {}. Forwarding request...", role);
                return chain.filter(exchange);
            });
        } catch (Exception e) {
            log.error("Token Error: {}", e.getMessage());
            return chain.filter(exchange);
        }
    }

    // NO need to put this in ENV as they are eureka based not changing
    private String getBaseUrl(String role) {
        return switch (role) {
            case "PATIENT" -> "lb://PATIENT-SERVICE/api/patients";
            case "DOCTOR" -> "lb://DOCTOR-SERVICE/api/doctors";
            case "THERAPIST" -> "lb://THERAPIST-SERVICE/api/therapists";
            default -> null;
        };
    }
}
