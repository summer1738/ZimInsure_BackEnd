package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.service.UserService;
import com.ziminsure.insurance.api.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import com.ziminsure.insurance.api.dto.ClientWithCarsDTO;
import com.ziminsure.insurance.domain.Car;
import java.util.List;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/auth", "/auth"})
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody ClientWithCarsDTO dto) {
        User user = dto.getClient();
        List<Car> cars = dto.getCars();
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        if (cars == null || cars.isEmpty()) {
            return ResponseEntity.badRequest().body("At least one car is required for registration");
        }
        user.setRole(User.Role.CLIENT);
        User saved = userService.registerUserWithCars(user, cars);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        System.out.println("Login attempt for email: " + loginRequest.getEmail());
        Optional<User> userOpt = userService.findByEmail(loginRequest.getEmail());
        if (userOpt.isEmpty()) {
            System.out.println("No user found for email: " + loginRequest.getEmail());
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        boolean passwordMatch = userService.checkPassword(userOpt.get(), loginRequest.getPassword());
        System.out.println("Password match for " + loginRequest.getEmail() + ": " + passwordMatch);
        if (!passwordMatch) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        User user = userOpt.get();
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.ok().body(Map.of(
            "token", token, 
            "role", user.getRole().name(),
            "username", user.getFullName(),
            "passwordChangeRequired", user.isPasswordChangeRequired()
        ));
    }

    @GetMapping("/api/test-auth")
    public ResponseEntity<String> testAuth() {
        return ResponseEntity.ok("Token is valid and user is authenticated!");
    }

    // Example: Only SUPER_ADMIN can create an agent
    @PostMapping("/agents")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createAgent(@RequestBody User agent) {
        // Implementation for creating an agent goes here
        return ResponseEntity.ok("Agent created (stub)");
    }
} 