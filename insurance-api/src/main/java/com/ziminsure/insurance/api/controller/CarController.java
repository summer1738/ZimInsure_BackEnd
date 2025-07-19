package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
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
@RequestMapping("/api/cars")
public class CarController {
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private UserService userService;

    // List cars for current client
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<List<Car>> listCars(@RequestParam(name = "clientId", required = false) Long clientId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.SUPER_ADMIN) {
            return ResponseEntity.ok(carRepository.findAll());
        } else if (user.getRole() == User.Role.CLIENT) {
            return ResponseEntity.ok(carRepository.findByClient(user));
        } else if (user.getRole() == User.Role.AGENT && clientId != null) {
            Optional<User> client = userService.findById(clientId);
            if (client.isPresent() && client.get().getCreatedBy() == user.getId()) {
                return ResponseEntity.ok(carRepository.findByClient(client.get()));
            } else {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.status(403).build();
    }

    // Add car
    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT')")
    public ResponseEntity<?> addCar(@RequestBody Car car, Principal principal) {
        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User user = userOpt.get();
        if (user.getRole() == User.Role.CLIENT) {
            car.setClient(user);
        } else if (user.getRole() == User.Role.AGENT && car.getClient() != null) {
            Optional<User> client = userService.findById(car.getClient().getId());
            if (client.isPresent() && client.get().getCreatedBy() == user.getId()) {
                car.setClient(client.get());
            } else {
                return ResponseEntity.status(403).body("Not allowed to add car for this client");
            }
        } else {
            return ResponseEntity.status(403).build();
        }
        Car saved = carRepository.save(car);
        return ResponseEntity.ok(saved);
    }

    // Update car
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT')")
    public ResponseEntity<?> updateCar(@PathVariable("id") Long id, @RequestBody Car car, Principal principal) {
        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User user = userOpt.get();
        Optional<Car> existingOpt = carRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();
        Car existing = existingOpt.get();
        if (user.getRole() == User.Role.CLIENT && !existing.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        if (user.getRole() == User.Role.AGENT) {
            Optional<User> client = userService.findById(existing.getClient().getId());
            if (client.isEmpty() || client.get().getCreatedBy() != user.getId()) {
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
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT')")
    public ResponseEntity<?> deleteCar(@PathVariable("id") Long id, Principal principal) {
        Optional<User> userOpt = userService.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User user = userOpt.get();
        Optional<Car> carOpt = carRepository.findById(id);
        if (carOpt.isEmpty()) return ResponseEntity.notFound().build();
        Car car = carOpt.get();
        if (user.getRole() == User.Role.CLIENT && !car.getClient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        if (user.getRole() == User.Role.AGENT) {
            Optional<User> client = userService.findById(car.getClient().getId());
            if (client.isEmpty() || client.get().getCreatedBy() != user.getId()) {
                return ResponseEntity.status(403).build();
            }
        }
        carRepository.deleteById(id);
        return ResponseEntity.ok("Car deleted");
    }
} 