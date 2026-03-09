package com.ruwanthi.pet_clinic.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.io.PrintWriter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // For now, we'll use basic defaults and tighten later.
                .csrf(csrf -> csrf.disable()) // we'll discuss proper CSRF later (important!)
                .cors(Customizer.withDefaults())

                // Session-based auth (we want sessions)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))

                // Custom authentication entry point to prevent browser popup
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            PrintWriter writer = response.getWriter();
                            writer.write("{\"error\": \"Unauthorized\", \"message\": \"Please login to access this resource\"}");
                            writer.flush();
                        })
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight
                        .requestMatchers(HttpMethod.GET, "/api/pets/images/**").permitAll() // pet images public
                        // Protected endpoints - require authentication
                        .requestMatchers("/api/pets/**").authenticated()
                        .requestMatchers("/api/owner/**").authenticated()
                        .requestMatchers("/api/pet-owner/**").authenticated()
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .requestMatchers("/api/appointments/**").authenticated()
                        .requestMatchers("/api/records/**").authenticated()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}

