package com.ruwanthi.pet_clinic.auth.web;

import com.ruwanthi.pet_clinic.auth.dto.MeResponse;
import com.ruwanthi.pet_clinic.auth.dto.SignupRequest;
import com.ruwanthi.pet_clinic.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.ruwanthi.pet_clinic.auth.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import com.ruwanthi.pet_clinic.auth.dto.LoginRequest;
import org.springframework.security.web.context.SecurityContextRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;





import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService,
                          SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        authService.registerPetOwner(request);
        return ResponseEntity.ok("Signup successful");
    }

    // We'll use this after login is implemented
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        String email = auth.getName();
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))   // keep only real roles
                .collect(Collectors.toSet());


        return ResponseEntity.ok(new MeResponse(email, roles));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {

        // 1) authenticate using AuthService
        Authentication authentication = authService.login(
                request.getEmail(),
                request.getPassword()
        );

        // 2) create a SecurityContext and set the Authentication
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 3) save context to session via SecurityContextRepository
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        // 4) ensure session is created (should already happen, but safe)
        httpRequest.getSession(true);

        return ResponseEntity.ok("Login successful");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logged out");
    }




}
