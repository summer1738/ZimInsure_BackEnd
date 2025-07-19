package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.domain.Policy;
import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.domain.PolicyResponse;
import com.ziminsure.insurance.repository.PolicyRepository;
import com.ziminsure.insurance.repository.CarRepository;
import com.ziminsure.insurance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {
    @Autowired
    private PolicyRepository policyRepository;
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private UserService userService;

    // List policies
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<List<PolicyResponse>> listPolicies(@RequestParam(name = "clientId", required = false) Long clientId, @RequestParam(name = "carId", required = false) Long carId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Policy> policies;
        if (user.getRole() == User.Role.CLIENT) {
            policies = policyRepository.findByClient(user);
        } else if (user.getRole() == User.Role.AGENT && clientId != null) {
            Optional<User> client = userService.findById(clientId);
            if (client.isPresent() && client.get().getCreatedBy() == user.getId()) {
                policies = policyRepository.findByClient(client.get());
            } else {
                return ResponseEntity.status(403).build();
            }
        } else if (user.getRole() == User.Role.SUPER_ADMIN) {
            if (clientId != null) {
                Optional<User> client = userService.findById(clientId);
                if (client.isPresent()) {
                    policies = policyRepository.findByClient(client.get());
                } else {
                    policies = List.of();
                }
            } else if (carId != null) {
                Optional<Car> car = carRepository.findById(carId);
                if (car.isPresent()) {
                    policies = policyRepository.findByCar(car.get());
                } else {
                    policies = List.of();
                }
            } else {
                policies = policyRepository.findAll();
            }
        } else {
            return ResponseEntity.status(403).build();
        }
        List<PolicyResponse> response = policies.stream().map(p -> new PolicyResponse(
            p.getId(),
            p.getPolicyNumber(),
            p.getType(),
            p.getStatus(),
            p.getStartDate(),
            p.getEndDate(),
            p.getPremium(),
            p.getCar() != null ? p.getCar().getId() : null,
            p.getClient() != null ? p.getClient().getId() : null,
            p.getClient() != null ? p.getClient().getFullName() : null
        )).toList();
        return ResponseEntity.ok(response);
    }

    // Create policy
    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> createPolicy(@RequestBody Policy policy, Principal principal) {
        if (policy.getCar() == null || policy.getCar().getId() == null) {
            return ResponseEntity.badRequest().body("Car is required");
        }
        if (policy.getClient() == null || policy.getClient().getId() == null) {
            return ResponseEntity.badRequest().body("Client is required");
        }
        Optional<Car> car = carRepository.findById(policy.getCar().getId());
        Optional<User> client = userService.findById(policy.getClient().getId());
        if (car.isEmpty() || client.isEmpty()) return ResponseEntity.badRequest().body("Car or client not found");
        policy.setCar(car.get());
        policy.setClient(client.get());
        Policy saved = policyRepository.save(policy);
        return ResponseEntity.ok(saved);
    }

    // Update policy
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> updatePolicy(@PathVariable("id") Long id, @RequestBody Policy policy, Principal principal) {
        Optional<Policy> existingOpt = policyRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();
        Policy existing = existingOpt.get();
        existing.setPolicyNumber(policy.getPolicyNumber());
        existing.setType(policy.getType());
        existing.setStatus(policy.getStatus());
        existing.setStartDate(policy.getStartDate());
        existing.setEndDate(policy.getEndDate());
        // Optionally update car/client if needed
        Policy saved = policyRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    // Delete policy
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> deletePolicy(@PathVariable("id") Long id) {
        if (!policyRepository.existsById(id)) return ResponseEntity.notFound().build();
        policyRepository.deleteById(id);
        return ResponseEntity.ok("Policy deleted");
    }
} 