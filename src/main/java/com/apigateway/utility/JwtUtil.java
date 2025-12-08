package com.apigateway.utility;

import com.apigateway.Security.JwtKeyProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtKeyProvider jwtKeyProvider;

    private Claims extractClaims(String token){
        try{
            token = token.replace("Bearer ","");

            //Step 1 : Extract Kid without verifying signature
            String[] parts = token.split("\\.");
            // Extract the encoded header part from the JWT
            String encodedHeader = parts[0];

            //decode base64 URL - encoded header
            byte[] decodedHeader = Base64.getUrlDecoder().decode(encodedHeader);

            //convert it to string
            String headerJson = new String(decodedHeader);

            //Create ObjectMapper instance from this
            ObjectMapper objectMapper = new ObjectMapper();

            // Parse the JSON string into a JsonNode
            JsonNode headerNode = objectMapper.readTree(headerJson);

            String kid = headerNode.get("kid").asText();


            //Step 2 : Fetch correct Public key based on KID
            PublicKey publicKey = jwtKeyProvider.getPublicKey(kid);


            //Step 3 : Verify and extract claims
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid Token: " + ex.getMessage());
        }
    }
    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }
    public String getRole(String token) {
        Claims claims = extractClaims(token);
        Map access = claims.get("realm_access", Map.class);
        List<String> roles = (List<String>) access.get("roles");
        if (roles.contains("PATIENT")) return "PATIENT";
        if (roles.contains("DOCTOR")) return "DOCTOR";
        if (roles.contains("THERAPIST")) return "THERAPIST";
        return null;
    }
}
