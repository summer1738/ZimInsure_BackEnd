package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.domain.Quotation;
import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.repository.QuotationRepository;
import com.ziminsure.insurance.repository.CarRepository;
import com.ziminsure.insurance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/quotations")
public class QuotationController {
    @Autowired
    private QuotationRepository quotationRepository;
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private UserService userService;

    // List quotations
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<List<Quotation>> listQuotations(@RequestParam(name = "clientId", required = false) Long clientId, @RequestParam(name = "agentId", required = false) Long agentId, @RequestParam(name = "carId", required = false) Long carId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.CLIENT) {
            return ResponseEntity.ok(quotationRepository.findByClient(user));
        } else if (user.getRole() == User.Role.AGENT) {
            if (clientId != null) {
                Optional<User> client = userService.findById(clientId);
                if (client.isPresent() && client.get().getCreatedBy() == user.getId()) {
                    return ResponseEntity.ok(quotationRepository.findByClient(client.get()));
                } else {
                    return ResponseEntity.status(403).build();
                }
            }
            return ResponseEntity.ok(quotationRepository.findByAgent(user));
        } else if (user.getRole() == User.Role.SUPER_ADMIN) {
            if (clientId != null) {
                Optional<User> client = userService.findById(clientId);
                if (client.isPresent()) {
                    return ResponseEntity.ok(quotationRepository.findByClient(client.get()));
                }
            } else if (agentId != null) {
                Optional<User> agent = userService.findById(agentId);
                if (agent.isPresent()) {
                    return ResponseEntity.ok(quotationRepository.findByAgent(agent.get()));
                }
            } else if (carId != null) {
                Optional<Car> car = carRepository.findById(carId);
                if (car.isPresent()) {
                    return ResponseEntity.ok(quotationRepository.findByCar(car.get()));
                }
            }
            return ResponseEntity.ok(quotationRepository.findAll());
        }
        return ResponseEntity.status(403).build();
    }

    // Create quotation
    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> createQuotation(@RequestBody Quotation quotation, Principal principal) {
        if (quotation.getCar() == null || quotation.getCar().getId() == null) {
            return ResponseEntity.badRequest().body("Car is required");
        }
        Optional<Car> car = carRepository.findById(quotation.getCar().getId());
        if (car.isEmpty()) return ResponseEntity.badRequest().body("Car not found");
        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User user = userOpt.get();
        if (user.getRole() == User.Role.CLIENT) {
            quotation.setClient(user);
            quotation.setCreatedDate(LocalDate.now());
        } else if (user.getRole() == User.Role.AGENT) {
            if (quotation.getClient() == null || quotation.getClient().getId() == null) {
                return ResponseEntity.badRequest().body("Client is required");
            }
            Optional<User> client = userService.findById(quotation.getClient().getId());
            if (client.isEmpty() || client.get().getCreatedBy() != user.getId()) {
                return ResponseEntity.status(403).body("Not allowed to create quotation for this client");
            }
            quotation.setAgent(user);
            quotation.setCreatedDate(LocalDate.now());
        } else if (user.getRole() == User.Role.SUPER_ADMIN) {
            // SUPER_ADMIN can create for any
            quotation.setCreatedDate(LocalDate.now());
        }
        quotation.setCar(car.get());
        Quotation saved = quotationRepository.save(quotation);
        return ResponseEntity.ok(saved);
    }

    // Update quotation
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateQuotation(@PathVariable("id") Long id, @RequestBody Quotation quotation, Principal principal) {
        Optional<Quotation> existingOpt = quotationRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();
        Quotation existing = existingOpt.get();
        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User user = userOpt.get();
        if (user.getRole() == User.Role.AGENT) {
            if (existing.getAgent() == null || !existing.getAgent().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        // SUPER_ADMIN can update any
        existing.setPolicyType(quotation.getPolicyType());
        existing.setStatus(quotation.getStatus());
        existing.setAmount(quotation.getAmount());
        Quotation saved = quotationRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    // Delete quotation
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteQuotation(@PathVariable("id") Long id, Principal principal) {
        Optional<Quotation> quotationOpt = quotationRepository.findById(id);
        if (quotationOpt.isEmpty()) return ResponseEntity.notFound().build();
        Quotation quotation = quotationOpt.get();
        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User user = userOpt.get();
        if (user.getRole() == User.Role.AGENT) {
            if (quotation.getAgent() == null || !quotation.getAgent().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        // SUPER_ADMIN can delete any
        quotationRepository.deleteById(id);
        return ResponseEntity.ok("Quotation deleted");
    }
} 