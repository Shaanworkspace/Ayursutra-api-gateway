package com.apigateway.Security;


import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.PublicKey;

@Slf4j
@Component
public class JwtKeyProvider {

    @Value("${keycloak.realm.jwks-url}")
    private String jwksUrl;

    private JWKSet cachedJwkSet; // store full jwk set

    public PublicKey getPublicKey(String kid) {
        try {
            // Load & cache on first call
            if (cachedJwkSet == null) {
                log.info("Fetching JWKS from: {}", jwksUrl);
                cachedJwkSet = JWKSet.load(new URL(jwksUrl));
                log.info("JWKS Loaded. Total Keys: {}", cachedJwkSet.getKeys().size());
            }

            // Find by key ID
            JWK jwk = cachedJwkSet.getKeyByKeyId(kid);

            // If not found -> refresh (rotation case)
            if (jwk == null) {
                log.warn("♻ Key ID not found. Refreshing JWKS...");
                cachedJwkSet = JWKSet.load(new URL(jwksUrl));
                jwk = cachedJwkSet.getKeyByKeyId(kid);
            }

            if (jwk == null) {
                throw new RuntimeException("Public Key with kid: " + kid + " not found!");
            }

            return ((RSAKey) jwk).toPublicKey();

        } catch (Exception ex) {
            log.error("!!!! Failed to load Keycloak Public Key from JWKS: {}", ex.getMessage());
            throw new RuntimeException("Failed to load Keycloak public key: " + ex.getMessage());
        }
    }
}
