package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.repository.CarRepository;
import com.ziminsure.insurance.service.UserService;
import com.ziminsure.insurance.service.InsuranceTermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/cars")
public class CarController {
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private InsuranceTermService insuranceTermService;

    // List cars for current client
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<List<Car>> listCars(@RequestParam(name = "clientId", required = false) Long clientId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        System.out.println("[DEBUG] User: " + user.getEmail() + ", Role: " + user.getRole());
        System.out.println("[DEBUG] Authorities: " + authentication.getAuthorities());
        if (user.getRole() == User.Role.SUPER_ADMIN) {
            return ResponseEntity.ok(carRepository.findAll());
        } else if (user.getRole() == User.Role.CLIENT) {
            return ResponseEntity.ok(carRepository.findByClient(user));
        } else if (user.getRole() == User.Role.AGENT) {
            if (clientId != null) {
                Optional<User> client = userService.findById(clientId);
                if (client.isPresent() && java.util.Objects.equals(client.get().getCreatedBy(), user.getId())) {
                    return ResponseEntity.ok(carRepository.findByClient(client.get()));
                } else {
                    return ResponseEntity.status(403).build();
                }
            } else {
                // Agent without clientId: return all cars for their assigned clients (e.g. dashboard)
                List<User> agentClients = userService.findClientsByAgent(user.getId());
                List<Car> cars = agentClients.stream()
                    .flatMap(c -> carRepository.findByClient(c).stream())
                    .toList();
                return ResponseEntity.ok(cars);
            }
        }
        return ResponseEntity.status(403).build();
    }

    // Add car
    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> addCar(@RequestBody Car car, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        if (user.getRole() == User.Role.CLIENT) {
            car.setClient(user);
        } else if (user.getRole() == User.Role.AGENT) {
            if (car.getClient() != null) {
                Optional<User> client = userService.findById(car.getClient().getId());
                if (client.isPresent() && java.util.Objects.equals(client.get().getCreatedBy(), user.getId())) {
                    car.setClient(client.get());
                } else {
                    return ResponseEntity.status(403).body("Not allowed to add car for this client");
                }
            } else {
                return ResponseEntity.status(400).body("Client is required for AGENT");
            }
        } else if (user.getRole() == User.Role.SUPER_ADMIN) {
            if (car.getClient() == null) {
                return ResponseEntity.status(400).body("Client is required");
            }
            Optional<User> client = userService.findById(car.getClient().getId());
            if (client.isEmpty() || client.get().getRole() != User.Role.CLIENT) {
                return ResponseEntity.status(400).body("Client not found or not a client user");
            }
            car.setClient(client.get());
        } else {
            return ResponseEntity.status(403).build();
        }
        
        Car saved = carRepository.save(car);
        insuranceTermService.createDefaultInsuranceTerm(saved);
        return ResponseEntity.ok(saved);
    }

    // Update car
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateCar(@PathVariable("id") Long id, @RequestBody Car car, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<Car> existingOpt = carRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();
        Car existing = existingOpt.get();
        if (user.getRole() == User.Role.SUPER_ADMIN) {
            // SUPER_ADMIN can update any car
        } else if (existing.getClient() == null) {
            return ResponseEntity.status(403).build(); // only SUPER_ADMIN can update cars with no client
        } else if (user.getRole() == User.Role.CLIENT && !existing.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        } else if (user.getRole() == User.Role.AGENT) {
            Optional<User> client = userService.findById(existing.getClient().getId());
            if (client.isEmpty() || !java.util.Objects.equals(client.get().getCreatedBy(), user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        // Update fields
        existing.setRegNumber(car.getRegNumber());
        existing.setMake(car.getMake());
        existing.setModel(car.getModel());
        existing.setYear(car.getYear());
        existing.setOwner(car.getOwner());
        existing.setStatus(car.getStatus());
        Car saved = carRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    // Delete car
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteCar(@PathVariable("id") Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<Car> carOpt = carRepository.findById(id);
        if (carOpt.isEmpty()) return ResponseEntity.notFound().build();
        Car car = carOpt.get();
        if (user.getRole() == User.Role.SUPER_ADMIN) {
            // SUPER_ADMIN can delete any car
        } else if (car.getClient() == null) {
            return ResponseEntity.status(403).build(); // only SUPER_ADMIN can delete cars with no client
        } else if (user.getRole() == User.Role.CLIENT && !car.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        } else if (user.getRole() == User.Role.AGENT) {
            Optional<User> client = userService.findById(car.getClient().getId());
            if (client.isEmpty() || !java.util.Objects.equals(client.get().getCreatedBy(), user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        carRepository.deleteById(id);
        return ResponseEntity.ok("Car deleted");
    }
} 