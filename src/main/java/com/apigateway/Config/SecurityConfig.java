package com.apigateway.Config;

import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http){
		http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				// CORSFilter is plugged here
				.cors(Customizer.withDefaults())
				.authorizeExchange(exchange->exchange
						.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.pathMatchers("/api/**","/**").permitAll()
						.anyExchange().authenticated());
		return http.build();
	}
}
