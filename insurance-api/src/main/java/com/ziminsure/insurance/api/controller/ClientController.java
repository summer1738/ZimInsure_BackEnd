package com.ziminsure.insurance.api.controller;

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
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> createClient(@RequestBody User client, Principal principal) {
        Optional<User> agentOpt = userService.findByEmail(principal.getName());
        if (agentOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User agent = agentOpt.get();
        client.setRole(User.Role.CLIENT);
        client.setCreatedBy(agent.getId());
        client.setPassword("ziminsure");
        client.setPasswordChangeRequired(true);
        User saved = userService.registerUser(client);
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
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> updateClient(@PathVariable("id") Long id, @RequestBody User client, Principal principal) {
        Optional<User> agentOpt = userService.findByEmail(principal.getName());
        if (agentOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User agent = agentOpt.get();
        User updated = userService.updateClientByAgent(id, client, agent.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> deleteClient(@PathVariable("id") Long id, Principal principal) {
        Optional<User> agentOpt = userService.findByEmail(principal.getName());
        if (agentOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found or not authenticated");
        }
        User agent = agentOpt.get();
        userService.deleteClientByAgent(id, agent.getId());
        return ResponseEntity.ok("Client deleted");
    }

    // Client self-profile endpoints
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CLIENT', 'SUPER_ADMIN')")
    public ResponseEntity<User> getMyProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        System.out.println("Authenticated user: " + user.getEmail());
        return ResponseEntity.ok(user);
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