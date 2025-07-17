package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AgentController {
    private final UserService userService;
    public AgentController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createAgent(@RequestBody User agent) {
        agent.setRole(User.Role.AGENT);
        User saved = userService.registerUser(agent);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<User>> listAgents() {
        List<User> agents = userService.findByRole(User.Role.AGENT);
        return ResponseEntity.ok(agents);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAgent(@PathVariable("id") Long id, @RequestBody User agent) {
        User updated = userService.updateUser(id, agent, User.Role.AGENT);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAgent(@PathVariable("id") Long id) {
        userService.deleteUser(id, User.Role.AGENT);
        return ResponseEntity.ok("Agent deleted");
    }
} 