package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.domain.InsuranceTerm;
import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.repository.InsuranceTermRepository;
import com.ziminsure.insurance.repository.CarRepository;
import com.ziminsure.insurance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/insurance-terms")
public class InsuranceTermController {
    @Autowired
    private InsuranceTermRepository insuranceTermRepository;
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<List<InsuranceTerm>> listTerms(@RequestParam(name = "carId") Long carId, Principal principal) {
        Optional<Car> car = carRepository.findById(carId);
        if (car.isEmpty()) return ResponseEntity.notFound().build();
        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(null);
        }
        User user = userOpt.get();
        if (user.getRole() == User.Role.CLIENT) {
            // CLIENT can only view terms for their own cars
            if (!car.get().getClient().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
        } else if (user.getRole() == User.Role.AGENT) {
            // AGENT can only view terms for cars of their clients
            User client = car.get().getClient();
            if (client.getCreatedBy() != user.getId()) {
                return ResponseEntity.status(403).build();
            }
        }
        // SUPER_ADMIN can view any
        return ResponseEntity.ok(insuranceTermRepository.findByCar(car.get()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> addTerm(@RequestBody InsuranceTerm term, Principal principal) {
        if (term.getCar() == null || term.getCar().getId() == null) {
            return ResponseEntity.badRequest().body("Car is required");
        }
        Optional<Car> car = carRepository.findById(term.getCar().getId());
        if (car.isEmpty()) return ResponseEntity.badRequest().body("Car not found");
        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User user = userOpt.get();
        if (user.getRole() == User.Role.AGENT) {
            User client = car.get().getClient();
            if (client.getCreatedBy() != user.getId()) {
                return ResponseEntity.status(403).body("Not allowed to insure car for this client");
            }
        }
        // SUPER_ADMIN can add for any car
        term.setCar(car.get());
        InsuranceTerm saved = insuranceTermRepository.save(term);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateTerm(@PathVariable("id") Long id, @RequestBody InsuranceTerm term, Principal principal) {
        Optional<InsuranceTerm> existingOpt = insuranceTermRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();
        InsuranceTerm existing = existingOpt.get();
        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User user = userOpt.get();
        if (user.getRole() == User.Role.AGENT) {
            User client = existing.getCar().getClient();
            if (client.getCreatedBy() != user.getId()) {
                return ResponseEntity.status(403).build();
            }
        }
        // SUPER_ADMIN can update any
        existing.setStartDate(term.getStartDate());
        existing.setEndDate(term.getEndDate());
        existing.setTermCount(term.getTermCount());
        InsuranceTerm saved = insuranceTermRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteTerm(@PathVariable("id") Long id, Principal principal) {
        Optional<InsuranceTerm> termOpt = insuranceTermRepository.findById(id);
        if (termOpt.isEmpty()) return ResponseEntity.notFound().build();
        InsuranceTerm term = termOpt.get();
        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User user = userOpt.get();
        if (user.getRole() == User.Role.AGENT) {
            User client = term.getCar().getClient();
            if (client.getCreatedBy() != user.getId()) {
                return ResponseEntity.status(403).build();
            }
        }
        // SUPER_ADMIN can delete any
        insuranceTermRepository.deleteById(id);
        return ResponseEntity.ok("Insurance term deleted");
    }
} 