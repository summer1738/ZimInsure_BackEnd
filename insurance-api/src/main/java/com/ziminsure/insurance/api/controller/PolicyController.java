package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.api.dto.PolicyRequestDTO;
import com.ziminsure.insurance.domain.Policy;
import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.domain.PolicyResponse;
import com.ziminsure.insurance.repository.PolicyRepository;
import com.ziminsure.insurance.repository.CarRepository;
import com.ziminsure.insurance.service.UserService;
import com.ziminsure.insurance.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @Autowired
    private NotificationService notificationService;

    // List policies
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<List<PolicyResponse>> listPolicies(@RequestParam(name = "clientId", required = false) Long clientId, @RequestParam(name = "carId", required = false) Long carId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Policy> policies;
        if (user.getRole() == User.Role.CLIENT) {
            policies = policyRepository.findByClient(user);
        } else if (user.getRole() == User.Role.AGENT) {
            if (clientId != null) {
                Optional<User> client = userService.findById(clientId);
                if (client.isPresent() && java.util.Objects.equals(client.get().getCreatedBy(), user.getId())) {
                    policies = policyRepository.findByClient(client.get());
                } else {
                    return ResponseEntity.status(403).build();
                }
            } else {
                // Agent without clientId: return all policies for their assigned clients (e.g. dashboard)
                List<User> agentClients = userService.findClientsByAgent(user.getId());
                policies = agentClients.stream()
                    .flatMap(c -> policyRepository.findByClient(c).stream())
                    .toList();
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
            p.getClient() != null ? p.getClient().getFullName() : null,
            p.getInsuranceCompany()
        )).toList();
        return ResponseEntity.ok(response);
    }

    // Create policy
    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> createPolicy(@RequestBody PolicyRequestDTO dto, Authentication authentication) {
        if (dto.getCarId() == null) {
            return ResponseEntity.badRequest().body("Car is required");
        }
        if (dto.getClientId() == null) {
            return ResponseEntity.badRequest().body("Client is required");
        }
        Optional<Car> car = carRepository.findById(dto.getCarId());
        Optional<User> client = userService.findById(dto.getClientId());
        if (car.isEmpty() || client.isEmpty()) return ResponseEntity.badRequest().body("Car or client not found");
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.AGENT && !user.getId().equals(client.get().getCreatedBy())) {
            return ResponseEntity.status(403).body("Not allowed to create policy for this client");
        }
        Policy policy = new Policy();
        policy.setPolicyNumber(dto.getPolicyNumber());
        policy.setType(dto.getType());
        policy.setStatus(dto.getStatus());
        policy.setStartDate(dto.getStartDate());
        policy.setEndDate(dto.getEndDate());
        policy.setPremium(dto.getPremium());
        policy.setInsuranceCompany(dto.getInsuranceCompany());
        policy.setCar(car.get());
        policy.setClient(client.get());
        Policy saved = policyRepository.save(policy);

        // Notify client about new policy
        User clientUser = saved.getClient();
        if (clientUser != null) {
            String reg = saved.getCar() != null && saved.getCar().getRegNumber() != null
                    ? saved.getCar().getRegNumber()
                    : "your car";
            String msg = String.format(
                    "A new policy %s has been created for %s with %s. Premium: %.2f.",
                    saved.getPolicyNumber(),
                    reg,
                    saved.getInsuranceCompany() != null ? saved.getInsuranceCompany() : "your insurer",
                    saved.getPremium() != null ? saved.getPremium() : 0.0
            );
            notificationService.addNotification(
                    msg,
                    "success",
                    "CLIENT",
                    null,
                    clientUser.getId(),
                    saved.getCar() != null ? saved.getCar().getId() : null
            );
        }

        return ResponseEntity.ok(saved);
    }

    // Update policy
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> updatePolicy(@PathVariable("id") Long id, @RequestBody PolicyRequestDTO dto, Authentication authentication) {
        Optional<Policy> existingOpt = policyRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();
        Policy existing = existingOpt.get();
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.AGENT) {
            if (existing.getClient() == null || !user.getId().equals(existing.getClient().getCreatedBy())) {
                return ResponseEntity.status(403).build();
            }
        }
        if (dto.getPolicyNumber() != null) existing.setPolicyNumber(dto.getPolicyNumber());
        if (dto.getType() != null) existing.setType(dto.getType());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getStartDate() != null) existing.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) existing.setEndDate(dto.getEndDate());
        if (dto.getPremium() != null) existing.setPremium(dto.getPremium());
        if (dto.getInsuranceCompany() != null) existing.setInsuranceCompany(dto.getInsuranceCompany());
        Policy saved = policyRepository.save(existing);

        // Notify client about policy update
        User clientUser = saved.getClient();
        if (clientUser != null) {
            String reg = saved.getCar() != null && saved.getCar().getRegNumber() != null
                    ? saved.getCar().getRegNumber()
                    : "your car";
            String msg = String.format(
                    "Your policy %s for %s has been updated. Please review the details.",
                    saved.getPolicyNumber(),
                    reg
            );
            notificationService.addNotification(
                    msg,
                    "info",
                    "CLIENT",
                    null,
                    clientUser.getId(),
                    saved.getCar() != null ? saved.getCar().getId() : null
            );
        }

        return ResponseEntity.ok(saved);
    }

    // Delete policy
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> deletePolicy(@PathVariable("id") Long id, Authentication authentication) {
        if (!policyRepository.existsById(id)) return ResponseEntity.notFound().build();
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.AGENT) {
            Optional<Policy> policyOpt = policyRepository.findById(id);
            if (policyOpt.isEmpty() || policyOpt.get().getClient() == null || !user.getId().equals(policyOpt.get().getClient().getCreatedBy())) {
                return ResponseEntity.status(403).build();
            }
        }
        policyRepository.deleteById(id);
        return ResponseEntity.ok("Policy deleted");
    }
} 