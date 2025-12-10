package com.apigateway.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final WebClient webClient;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.admin-token-url}")
    private String keycloakTokenUrl;

    @Value("${keycloak.user-token-url}")
    private String keycloakUserTokenUrl;

    @Value("${keycloak.addUser-url}")
    private String keycloakUserAddUrl;

    // =========================
    // LOGIN
    // =========================
    /*
    @PostMapping("/login")

    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String password = req.get("password");

        if (email == null || password == null) {
            log.warn("Login failed: Missing email or password");
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Email and Password are required")));
        }

        log.info("Login request received for {}", email);
        log.debug("Requesting token from Keycloak: {}", keycloakUserTokenUrl);

        return webClient.post()
                .uri(keycloakUserTokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", clientId)
                        .with("username", email)
                        .with("password", password))
                .retrieve()

                // 4xx
                .onStatus(HttpStatusCode::is4xxClientError, res ->
                        res.bodyToMono(String.class).flatMap(error -> {
                            log.error("Login failed (4xx): {}", error);
                            return Mono.error(new RuntimeException(
                                    "Invalid credentials or client configuration: " + error));
                        })
                )

                // 5xx
                .onStatus(HttpStatusCode::is5xxServerError, res ->
                        res.bodyToMono(String.class).flatMap(error -> {
                            log.error("Keycloak server error (5xx): {}", error);
                            return Mono.error(new RuntimeException(
                                    "Keycloak server failure: " + error));
                        })
                )

                // success - Map.class se aayega jo mixed types support karta hai
                .bodyToMono(Map.class)
                .flatMap(tokenMap -> {
                    log.info("Login successful for {}", email);
                    return Mono.just(ResponseEntity.ok((Map<String, Object>) tokenMap));
                })

                // final catch
                .onErrorResume(ex -> {
                    log.error("Login exception: {}", ex.getMessage());
                    Map<String, Object> errorBody = Map.of(
                            "error", "login_failed",
                            "message", ex.getMessage()
                    );
                    return Mono.just(ResponseEntity.status(401).body(errorBody));
                });
    }
    */

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody Map<String, String> req) {

        // === 1. Extract and validate input ===
        String email = req.get("email");
        String password = req.get("password");

        if (email == null || password == null) {
            log.warn("Login failed: Missing email or password");
            Map<String, Object> err = Map.of("error", "Email and Password are required");
            return Mono.just(ResponseEntity.badRequest().body(err));
        }

        log.info("Login request received for {}", email);

        // === 2. Build Keycloak token request ===
        var formData = BodyInserters.fromFormData("grant_type", "password")
                .with("client_id", clientId)
                .with("username", email)
                .with("password", password);

        var keycloakRequest = webClient
                .post()
                .uri(keycloakUserTokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData);

        // === 3. Call Keycloak and handle possible errors ===
        return keycloakRequest
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, res ->
                        res.bodyToMono(String.class).flatMap(error -> {
                            log.error("Login failed (4xx): {}", error);
                            return Mono.error(new RuntimeException("Invalid credentials: " + error));
                        })
                )
                .onStatus(HttpStatusCode::is5xxServerError, res ->
                        res.bodyToMono(String.class).flatMap(error -> {
                            log.error("Keycloak server error (5xx): {}", error);
                            return Mono.error(new RuntimeException("Keycloak server error: " + error));
                        })
                )

                // === 4. Handle a successful response ===
                .bodyToMono(Map.class)
                .flatMap(tokenMap -> {

                    log.info("Login successful for {}", email);

                    // ---- 4A. Extract useful parts from Keycloak response ----
                    String accessToken = (String) tokenMap.get("access_token");
                    String refreshToken = (String) tokenMap.get("refresh_token");
                    String tokenType = (String) tokenMap.getOrDefault("token_type", "Bearer");
                    Integer accessExpiresIn = (Integer) tokenMap.get("expires_in");
                    Integer refreshExpiresIn = (Integer) tokenMap.get("refresh_expires_in");

                    // ---- 4B. Prepare response body to React (only access token) ----
                    Map<String, Object> responseBody = Map.of(
                            "access_token", accessToken,
                            "token_type", tokenType,
                            "expires_in", accessExpiresIn,
                            "message", "Login successful"
                    );

                    // ---- 4C. Create refresh token HttpOnly cookie ----
                    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                            .httpOnly(true)
                            .secure(true) // true for HTTPS prod
                            .path("/")
                            .sameSite("Strict")
                            .maxAge(Duration.ofSeconds(refreshExpiresIn))
                            .build();

                    // ---- 4D. Combine body + cookie into final response ----
                    return Mono.just(
                            ResponseEntity.ok()
                                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                                    .body(responseBody)
                    );
                })

                // === 5. Final universal error handling ===
                .onErrorResume(ex -> {
                    log.error("Login exception: {}", ex.getMessage());
                    Map<String, Object> errorBody = Map.of(
                            "error", "login_failed",
                            "message", ex.getMessage()
                    );
                    return Mono.just(ResponseEntity.status(401).body(errorBody));
                });
    }

    // =========================
    // REGISTER
    // =========================
    @PostMapping("/register")
    public Mono<ResponseEntity<?>> register(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String password = req.get("password");
        String role = req.get("role");
        String phone = (String) req.getOrDefault("phone", "9999999999");
        String firstName = (String) req.getOrDefault("firstName", "User");
        String lastName = (String) req.getOrDefault("lastName", "");

        // === 1. Validate required input fields ===
        if (email == null || password == null || role == null) {
            List<String> missingFields = new ArrayList<>();
            if (email == null) missingFields.add("email");
            if (password == null) missingFields.add("password");
            if (role == null) missingFields.add("role");

            String message = "Missing required field(s): " + String.join(", ", missingFields);
            log.warn("Registration Failed: {}", message);
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", message)));
        }

        log.info("Register request received for {}", email);
        log.debug("Requesting Admin Token from Keycloak...");

        // === 2. Get the Admin Access Token ===
        Mono<String> adminTokenMono = getAdminToken();

        // === 3. Use the admin token to create the user and assign a realm role ===
        return adminTokenMono.flatMap(adminToken -> {

            // ---- 3A. Build user JSON object for creation ----
            Map<String, Object> user = new HashMap<>();
            user.put("username", email);
            user.put("email", email);
            user.put("enabled", true);
            user.put("emailVerified", true);
            user.put("firstName", firstName);
            user.put("lastName", lastName);
            user.put("attributes", Map.of("phone", List.of(phone)));
            user.put("credentials", new Object[]{
                    Map.of("type", "password", "value", password, "temporary", false)
            });

            log.debug("Creating user {} in Keycloak realm...", email);

            // ---- 3B. Create the user ----
            return webClient.post()
                    .uri(keycloakUserAddUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(user)
                    .exchangeToMono(resp -> {
                        if (resp.statusCode().is2xxSuccessful()) {
                            // Extract user ID from "Location" header
                            String location = resp.headers().asHttpHeaders().getLocation().toString();
                            String userId = location.substring(location.lastIndexOf("/") + 1);
                            log.info("User created with ID: {}", userId);

                            // ---- 3C. Fetch the Role definition from the realm ----
                            String roleLookupUrl = "http://localhost:8180/admin/realms/ayursutra-realm/roles/" + role;
                            String assignRoleUrl = "http://localhost:8180/admin/realms/ayursutra-realm/users/"
                                    + userId + "/role-mappings/realm";

                            return webClient.get()
                                    .uri(roleLookupUrl)
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                    .retrieve()
                                    .bodyToMono(Map.class)
                                    .flatMap(roleRepresentation -> {
                                        log.debug("Assigning role {} to user {}", role, userId);

                                        // ---- 3D. Assign the role to the user ----
                                        return webClient.post()
                                                .uri(assignRoleUrl)
                                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(List.of(roleRepresentation))
                                                .retrieve()
                                                .toBodilessEntity()
                                                .map(v -> {
                                                    log.info("Role {} assigned to user {}", role, email);
                                                    return ResponseEntity.ok(
                                                            Map.of("message", "User Registered Successfully with role: " + role));
                                                });
                                    });
                        } else if (resp.statusCode().value() == 409) {
                            log.warn("User already exists: {}", email);
                            return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body(Map.of("error", "User Already Exists")));
                        } else {
                            return resp.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("User Creation Error: {}", body);
                                        return Mono.just(ResponseEntity.status(resp.statusCode())
                                                .body(Map.of("error", body)));
                                    });
                        }
                    })
                    .onErrorResume(err -> {
                        log.error("Registration failed: {}", err.getMessage());
                        return Mono.just(ResponseEntity.badRequest()
                                .body(Map.of("error", err.getMessage())));
                    });
        });
    }
    private Mono<String> getAdminToken() {
        return webClient.post()
                .uri(keycloakTokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", "admin-cli")
                        .with("username", "admin")
                        .with("password", "admin"))
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        res -> res.bodyToMono(String.class).flatMap(error -> {
                            log.error("Admin Authentication Failed: {}", error);
                            return Mono.error(new RuntimeException("Admin Credentials Invalid → " + error));
                        })
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        res -> res.bodyToMono(String.class).flatMap(error -> {
                            log.error("Keycloak Server Error (Admin Login): {}", error);
                            return Mono.error(new RuntimeException("Keycloak Server Issue → " + error));
                        })
                )
                .bodyToMono(Map.class)
                .map(res -> res.get("access_token").toString());
    }
}
