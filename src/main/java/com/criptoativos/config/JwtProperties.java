package com.criptoativos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Signing configuration for access tokens.
 *
 * <p>Keys are PEM {@link Resource}s rather than inline strings so the private key can live outside
 * the image and be mounted at deploy time.
 */
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(Resource publicKey, Resource privateKey, String issuer, Duration ttl) {}
