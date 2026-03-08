package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.domain.InsuranceTerm;
import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.repository.InsuranceTermRepository;
import com.ziminsure.insurance.repository.CarRepository;
import com.ziminsure.insurance.service.UserService;
import com.ziminsure.insurance.service.InsuranceTermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/insurance-terms")
public class InsuranceTermController {
    private final InsuranceTermRepository insuranceTermRepository;
    private final CarRepository carRepository;
    private final UserService userService;
    private final InsuranceTermService insuranceTermService;

    @Autowired
    public InsuranceTermController(InsuranceTermRepository insuranceTermRepository, 
                                 CarRepository carRepository, 
                                 UserService userService,
                                 InsuranceTermService insuranceTermService) {
        this.insuranceTermRepository = insuranceTermRepository;
        this.carRepository = carRepository;
        this.userService = userService;
        this.insuranceTermService = insuranceTermService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<List<InsuranceTerm>> getTerms(@RequestParam(required = false) Long carId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        if (carId != null) {
            Optional<Car> car = carRepository.findById(carId);
            if (car.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Check authorization
            if (user.getRole() == User.Role.CLIENT) {
                if (!car.get().getClient().getId().equals(user.getId())) {
                    return ResponseEntity.status(403).build();
                }
            } else if (user.getRole() == User.Role.AGENT) {
                if (!java.util.Objects.equals(car.get().getClient().getCreatedBy(), user.getId())) {
                    return ResponseEntity.status(403).build();
                }
            }
            // SUPER_ADMIN can access any car
            
            return ResponseEntity.ok(insuranceTermService.findByCar(car.get()));
        } else {
            // Return all terms for user's cars
            if (user.getRole() == User.Role.CLIENT) {
                List<Car> userCars = carRepository.findByClient(user);
                List<InsuranceTerm> allTerms = userCars.stream()
                    .flatMap(car -> insuranceTermService.findByCar(car).stream())
                    .toList();
                return ResponseEntity.ok(allTerms);
            } else if (user.getRole() == User.Role.AGENT) {
                // Return terms for agent's clients' cars
                List<Car> agentCars = carRepository.findAll().stream()
                    .filter(car -> java.util.Objects.equals(car.getClient().getCreatedBy(), user.getId()))
                    .toList();
                List<InsuranceTerm> allTerms = agentCars.stream()
                    .flatMap(car -> insuranceTermService.findByCar(car).stream())
                    .toList();
                return ResponseEntity.ok(allTerms);
            } else {
                // SUPER_ADMIN - return all terms
                return ResponseEntity.ok(insuranceTermRepository.findAll());
            }
        }
    }

    @GetMapping("/scan-expiring")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<List<InsuranceTerm>> scanExpiring(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(30);
        List<InsuranceTerm> allTerms = insuranceTermRepository.findAll();
        List<InsuranceTerm> expiring = allTerms.stream()
            .filter(t -> t.getEndDate() != null && !t.getEndDate().isBefore(today) && !t.getEndDate().isAfter(windowEnd))
            .toList();
        List<InsuranceTerm> allowed;
        if (user.getRole() == User.Role.CLIENT) {
            List<Car> userCars = carRepository.findByClient(user);
            Set<Long> carIds = userCars.stream().map(Car::getId).collect(Collectors.toSet());
            allowed = expiring.stream().filter(t -> carIds.contains(t.getCar().getId())).toList();
        } else if (user.getRole() == User.Role.AGENT) {
            allowed = expiring.stream()
                .filter(t -> user.getId().equals(t.getCar().getClient().getCreatedBy()))
                .toList();
        } else {
            allowed = expiring;
        }
        return ResponseEntity.ok(allowed);
    }

    @GetMapping("/is-insured")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<Boolean> isInsured(@RequestParam("carId") Long carId, Authentication authentication) {
        Optional<Car> car = carRepository.findById(carId);
        if (car.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = (User) authentication.getPrincipal();
        
        // Check authorization based on user role
        if (user.getRole() == User.Role.CLIENT) {
            // CLIENT can only check their own cars
            if (!car.get().getClient().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
        } else if (user.getRole() == User.Role.AGENT) {
            // AGENT can only check cars of their clients
            if (!java.util.Objects.equals(car.get().getClient().getCreatedBy(), user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        // SUPER_ADMIN can check any car
        
        boolean isInsured = insuranceTermService.isCarInsured(car.get());
        return ResponseEntity.ok(isInsured);
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<InsuranceTerm> getCurrentTerm(@RequestParam("carId") Long carId, Authentication authentication) {
        Optional<Car> car = carRepository.findById(carId);
        if (car.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = (User) authentication.getPrincipal();
        
        // Check authorization based on user role
        if (user.getRole() == User.Role.CLIENT) {
            // CLIENT can only check their own cars
            if (!car.get().getClient().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
        } else if (user.getRole() == User.Role.AGENT) {
            // AGENT can only check cars of their clients
            if (!java.util.Objects.equals(car.get().getClient().getCreatedBy(), user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        // SUPER_ADMIN can check any car
        
        Optional<InsuranceTerm> currentTerm = insuranceTermService.findCurrentByCar(car.get());
        return currentTerm.map(ResponseEntity::ok).orElse(ResponseEntity.ok(null));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> addTerm(@RequestBody InsuranceTerm term, Authentication authentication) {
        if (term.getCar() == null || term.getCar().getId() == null) {
            return ResponseEntity.badRequest().body("Car is required");
        }
        Optional<Car> car = carRepository.findById(term.getCar().getId());
        if (car.isEmpty()) return ResponseEntity.badRequest().body("Car not found");
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.AGENT) {
            User client = car.get().getClient();
            if (client == null || !java.util.Objects.equals(client.getCreatedBy(), user.getId())) {
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
    public ResponseEntity<?> updateTerm(@PathVariable("id") Long id, @RequestBody InsuranceTerm term, Authentication authentication) {
        Optional<InsuranceTerm> existingOpt = insuranceTermRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();
        InsuranceTerm existing = existingOpt.get();
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.AGENT) {
            User client = existing.getCar().getClient();
            if (client == null || !java.util.Objects.equals(client.getCreatedBy(), user.getId())) {
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
    public ResponseEntity<?> deleteTerm(@PathVariable("id") Long id, Authentication authentication) {
        Optional<InsuranceTerm> termOpt = insuranceTermRepository.findById(id);
        if (termOpt.isEmpty()) return ResponseEntity.notFound().build();
        InsuranceTerm term = termOpt.get();
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.AGENT) {
            User client = term.getCar().getClient();
            if (client == null || !java.util.Objects.equals(client.getCreatedBy(), user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        // SUPER_ADMIN can delete any
        insuranceTermRepository.deleteById(id);
        return ResponseEntity.ok("Insurance term deleted");
    }
} 