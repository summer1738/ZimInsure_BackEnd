package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.api.dto.ClientWithCarsDTO;
import com.ziminsure.insurance.api.dto.UserProfileDTO;
import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
    private final UserService userService;

    public ClientController(UserService userService) {
        this.userService = userService;
    }

    // Agent endpoints
    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> createClient(@RequestBody ClientWithCarsDTO dto, Principal principal) {
        Optional<User> creatorOpt = userService.findByEmail(principal.getName());
        if (creatorOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User creator = creatorOpt.get();
        // Prevent CLIENT from creating another client
        if (creator.getRole() == User.Role.CLIENT) {
            return ResponseEntity.status(403).body("Clients are not allowed to create clients");
        }
        User client = dto.getClient();
        List<Car> cars = dto.getCars();
        if (cars == null || cars.isEmpty()) {
            return ResponseEntity.badRequest().body("At least one car is required to create a client");
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

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateClient(@PathVariable("id") Long id, @RequestBody User client,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        if (user.getRole() == User.Role.AGENT) {
            User updated = userService.updateClientByAgent(id, client, user.getId());
            return ResponseEntity.ok(updated);
        } else if (user.getRole() == User.Role.SUPER_ADMIN) {
            // SUPER_ADMIN can update any client
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
            if (client.getPassword() != null && !client.getPassword().isEmpty()) {
                existingClient.setPassword(userService.encodePassword(client.getPassword()));
            }
            User updated = userService.save(existingClient);
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteClient(@PathVariable("id") Long id, Authentication authentication) {
        System.out.println("DELETE /api/clients/" + id + " called by user: " + authentication.getName());
        User user = (User) authentication.getPrincipal();
        System.out.println("User role: " + user.getRole());

        if (user.getRole() == User.Role.AGENT) {
            System.out.println("Deleting client as AGENT");
            userService.deleteClientByAgent(id, user.getId());
            return ResponseEntity.ok("Client deleted");
        } else if (user.getRole() == User.Role.SUPER_ADMIN) {
            System.out.println("Deleting client as SUPER_ADMIN");
            // SUPER_ADMIN can delete any client
            User client = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            System.out.println("Found client: " + client.getEmail() + " with role: " + client.getRole());
            if (client.getRole() != User.Role.CLIENT) {
                System.out.println("User is not a client, returning bad request");
                return ResponseEntity.badRequest().body("User is not a client");
            }
            System.out.println("Calling userService.deleteById(" + id + ")");
            userService.deleteById(id);
            System.out.println("Client deleted successfully");
            return ResponseEntity.ok("Client deleted");
        } else {
            System.out.println("User role not authorized: " + user.getRole());
            return ResponseEntity.status(403).build();
        }
    }

    // Client self-profile endpoints
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CLIENT', 'SUPER_ADMIN')")
    public ResponseEntity<UserProfileDTO> getMyProfile(Authentication authentication) {
        User principal = (User) authentication.getPrincipal();
        User user = userService.findWithCarsByEmail(principal.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserProfileDTO dto = new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCars());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> updateMyProfile(@RequestBody User client, Principal principal) {
        Optional<User> currentOpt = userService.findByEmail(principal.getName());
        if (currentOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User current = currentOpt.get();
        User updated = userService.updateUser(current.getId(), client, User.Role.CLIENT);
        return ResponseEntity.ok(updated);
    }
}