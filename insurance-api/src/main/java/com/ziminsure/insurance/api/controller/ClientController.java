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
        // When SUPER_ADMIN creates a client, they may assign to an agent via client.getCreatedBy(); otherwise use creator.
        if (creator.getRole() == User.Role.SUPER_ADMIN && client.getCreatedBy() != null) {
            Optional<User> agent = userService.findById(client.getCreatedBy());
            if (agent.isPresent() && agent.get().getRole() == User.Role.AGENT) {
                client.setCreatedBy(agent.get().getId());
            } else {
                client.setCreatedBy(creator.getId());
            }
        } else {
            client.setCreatedBy(creator.getId());
        }
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
            // Reassign to another agent if SUPER_ADMIN sends createdBy (valid agent id)
            if (client.getCreatedBy() != null) {
                Optional<User> agent = userService.findById(client.getCreatedBy());
                if (agent.isPresent() && agent.get().getRole() == User.Role.AGENT) {
                    existingClient.setCreatedBy(agent.get().getId());
                }
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

    /** Reassign a client to another agent or super-admin (SUPER_ADMIN only). Updates only createdBy. */
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> assignClientToAgent(@PathVariable("id") Long id, @RequestBody java.util.Map<String, Long> body, Authentication authentication) {
        Long agentId = body != null ? body.get("agentId") : null;
        if (agentId == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "agentId is required"));
        }
        User existingClient = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        if (existingClient.getRole() != User.Role.CLIENT) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "User is not a client"));
        }
        Optional<User> assignable = userService.findById(agentId);
        if (assignable.isEmpty() || (assignable.get().getRole() != User.Role.AGENT && assignable.get().getRole() != User.Role.SUPER_ADMIN)) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Invalid agent id"));
        }
        existingClient.setCreatedBy(assignable.get().getId());
        userService.save(existingClient);
        return ResponseEntity.ok(userService.findById(id).orElse(existingClient));
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
        // For CLIENT: include assigned agent (createdBy) so client can see their agent on dashboard
        if (user.getRole() == User.Role.CLIENT && user.getCreatedBy() != null) {
            userService.findById(user.getCreatedBy()).ifPresent(agent -> {
                dto.setAssignedAgentId(agent.getId());
                dto.setAssignedAgentName(agent.getFullName());
                dto.setAssignedAgentEmail(agent.getEmail());
                dto.setAssignedAgentPhone(agent.getPhone());
            });
        }
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