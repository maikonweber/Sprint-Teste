package com.example.people.service;

import com.example.people.dto.auth.LoginRequest;
import com.example.people.dto.auth.LoginResponse;
import com.example.people.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            log.warn("Falha de login para o usuário '{}'", request.username());
            throw ex;
        }

        log.info("Login realizado: usuário '{}'", authentication.getName());
        String token = jwtService.generateToken(authentication.getName());
        return new LoginResponse(token, TOKEN_TYPE, jwtService.getExpiration().toSeconds());
    }
}
