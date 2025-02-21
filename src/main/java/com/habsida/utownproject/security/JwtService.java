package com.habsida.utownproject.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationTime;

    public String generateToken(String username, Set<String> roles) {
        return JWT.create()
                .withSubject(username)
                .withClaim("roles", roles.stream().toList())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))
                .sign(Algorithm.HMAC256(secret));
    }

    public String validateToken(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(secret))
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public List<String> getRolesFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(secret))
                    .build()
                    .verify(token);
            return decodedJWT.getClaim("roles").asList(String.class);
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public DecodedJWT extractClaims(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(secret))
                    .build()
                    .verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
