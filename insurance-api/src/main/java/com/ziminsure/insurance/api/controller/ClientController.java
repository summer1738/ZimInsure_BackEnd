package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.api.dto.ClientWithCarsDTO;
import com.ziminsure.insurance.api.dto.UserProfileDTO;
import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.repository.CarRepository;
import com.ziminsure.insurance.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
    private final UserService userService;
    private final CarRepository carRepository;

    public ClientController(UserService userService, CarRepository carRepository) {
        this.userService = userService;
        this.carRepository = carRepository;
    }

    // Agent endpoints
    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> createClient(@RequestBody ClientWithCarsDTO dto, Authentication authentication) {
        User creator = (User) authentication.getPrincipal();
        Optional<User> creatorOpt = userService.findByEmail(creator.getEmail());
        if (creatorOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        creator = creatorOpt.get();
        // Prevent CLIENT from creating another client
        if (creator.getRole() == User.Role.CLIENT) {
            return ResponseEntity.status(403).body("Clients are not allowed to create clients");
        }
        User client = dto.getClient();
        List<Car> cars = dto.getCars();
        if (cars == null || cars.isEmpty()) {
            return ResponseEntity.badRequest().body("At least one car is required to create a client");
        }
        for (Car car : cars) {
            if (car.getType() == null) {
                car.setType(Car.CarType.PRIVATE);
            }
        }
        client.setRole(User.Role.CLIENT);
        client.setCreatedBy(creator.getId());
        client.setPassword("ziminsure");
        client.setPasswordChangeRequired(true);
        User saved = userService.registerUserWithCars(client, cars);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<List<User>> listClients(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.SUPER_ADMIN) {
            return ResponseEntity.ok(userService.findByRole(User.Role.CLIENT));
        } else if (user.getRole() == User.Role.AGENT) {
            List<User> clients = userService.findClientsByAgent(user.getId());
            return ResponseEntity.ok(clients);
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> getClientWithCars(@PathVariable("id") Long id, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Optional<User> clientOpt = userService.findById(id);
        if (clientOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User client = clientOpt.get();
        if (client.getRole() != User.Role.CLIENT) {
            return ResponseEntity.badRequest().body("User is not a client");
        }
        if (currentUser.getRole() == User.Role.AGENT && !currentUser.getId().equals(client.getCreatedBy())) {
            return ResponseEntity.status(403).build();
        }
        List<Car> cars = carRepository.findByClientId(id);
        client.setCars(cars);
        return ResponseEntity.ok(client);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateClient(@PathVariable("id") Long id, @RequestBody ClientWithCarsDTO dto,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        User client = dto != null && dto.getClient() != null ? dto.getClient() : new User();
        List<Car> cars = dto != null ? dto.getCars() : null;

        if (user.getRole() == User.Role.AGENT) {
            User updated = userService.updateClientWithCars(id, client, cars, user.getId());
            return ResponseEntity.ok(updated);
        } else if (user.getRole() == User.Role.SUPER_ADMIN) {
            User existingClient = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            if (existingClient.getRole() != User.Role.CLIENT) {
                return ResponseEntity.badRequest().body("User is not a client");
            }
            existingClient.setFullName(client.getFullName());
            existingClient.setEmail(client.getEmail());
            existingClient.setIdNumber(client.getIdNumber());
            existingClient.setAddress(client.getAddress());
            existingClient.setPhone(client.getPhone());
            if (client.getStatus() != null) {
                existingClient.setStatus(client.getStatus());
            }
            if (client.getPassword() != null && !client.getPassword().isEmpty()) {
                existingClient.setPassword(userService.encodePassword(client.getPassword()));
            }
            userService.save(existingClient);
            if (cars != null && !cars.isEmpty()) {
                userService.syncClientCars(id, cars);
            }
            return ResponseEntity.ok(userService.findById(id).orElse(existingClient));
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteClient(@PathVariable("id") Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        if (user.getRole() == User.Role.AGENT) {
            try {
                userService.deleteClientByAgent(id, user.getId());
                return ResponseEntity.ok("Client deleted");
            } catch (Exception e) {
                return ResponseEntity.status(409).body("Cannot delete: " + e.getMessage());
            }
        } else if (user.getRole() == User.Role.SUPER_ADMIN) {
            User client = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            if (client.getRole() != User.Role.CLIENT) {
                return ResponseEntity.badRequest().body("User is not a client");
            }
            try {
                userService.deleteById(id);
                return ResponseEntity.ok("Client deleted");
            } catch (Exception e) {
                String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                if (msg != null && (msg.contains("foreign key") || msg.contains("constraint"))) {
                    return ResponseEntity.status(409).body("Cannot delete client: they have policies, quotations, or other linked data. Remove those first.");
                }
                return ResponseEntity.status(500).body("Delete failed: " + msg);
            }
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    // Client self-profile endpoints
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<UserProfileDTO> getMyProfile(Authentication authentication) {
        User principal = (User) authentication.getPrincipal();
        User user = userService.findWithCarsByEmail(principal.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserProfileDTO dto = new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getIdNumber(),
                user.getStatus(),
                user.getRole(),
                user.getCars());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateMyProfile(@RequestBody User client, Authentication authentication) {
        User current = (User) authentication.getPrincipal();
        Optional<User> currentOpt = userService.findById(current.getId());
        if (currentOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        current = currentOpt.get();
        User updated = userService.updateUser(current.getId(), client, current.getRole());
        return ResponseEntity.ok(updated);
    }
}