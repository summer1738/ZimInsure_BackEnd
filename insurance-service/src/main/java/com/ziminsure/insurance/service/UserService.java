package com.ziminsure.insurance.service;

import com.ziminsure.insurance.domain.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    User registerUser(User user);
    Optional<User> findByEmail(String email);
    boolean checkPassword(User user, String rawPassword);
    List<User> findByRole(User.Role role);
    User updateUser(Long id, User updatedUser, User.Role requiredRole);
    void deleteUser(Long id, User.Role requiredRole);
    List<User> findClientsByAgent(Long agentId);
    User updateClientByAgent(Long clientId, User updatedUser, Long agentId);
    void deleteClientByAgent(Long clientId, Long agentId);
    Optional<User> findById(Long id);
} 