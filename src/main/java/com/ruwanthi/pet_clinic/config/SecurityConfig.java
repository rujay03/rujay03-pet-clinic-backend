package com.ruwanthi.pet_clinic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // For now, we’ll use basic defaults and tighten later.
                .csrf(csrf -> csrf.disable()) // we'll discuss proper CSRF later (important!)
                .cors(Customizer.withDefaults())

                // Session-based auth (we want sessions)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))


                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight
                        // Temporary: Allow unauthenticated access to pet owner features
                        .requestMatchers("/api/owner/**").permitAll()
                        .requestMatchers("/api/pet-owner/**").permitAll()
                        .requestMatchers("/api/dashboard/**").permitAll()
                        .requestMatchers("/api/appointments/**").permitAll()
                        .requestMatchers("/api/records/**").permitAll()
                        .requestMatchers("/api/pets/**").permitAll()
                        .anyRequest().authenticated()
                )

                // We'll use formLogin later only if you want Spring default login page.
                // For API login, we will authenticate manually in controller.
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
