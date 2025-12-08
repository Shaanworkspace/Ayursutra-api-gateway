package com.apigateway.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserValidationService {

    private final WebClient.Builder webClientBuilder;

    /**
     * Check if user exists in Microservice Database
     * URL Example: lb://PATIENT-SERVICE/api/patients/exists
     */
    public Mono<Boolean> validateUser(String token,String existsUrl){
        return webClientBuilder.build()
                .get()
                .uri(existsUrl)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(error -> {
                    log.warn("Validate User Error: {} | URL: {}", error.getMessage(), existsUrl);
                    return Mono.just(false); // default → not exists
                });
    }

    /**
     * Sync user to Microservice DB if not present
     * URL Example: lb://PATIENT-SERVICE/api/patients/sync
     */
    public Mono<Void> syncUser(String token, String syncUrl) {
        return webClientBuilder.build()
                .post()
                .uri(syncUrl)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(error -> {
                    log.error("Sync User Error: {} | URL: {}", error.getMessage(), syncUrl);
                    return Mono.empty(); // do not break the request flow
                });
    }
}
