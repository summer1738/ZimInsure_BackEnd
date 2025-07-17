package com.ziminsure.insurance.service.impl;

import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.repository.UserRepository;
import com.ziminsure.insurance.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Override
    public List<User> findByRole(User.Role role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == role)
                .toList();
    }

    @Override
    public User updateUser(Long id, User updatedUser, User.Role requiredRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != requiredRole) {
            throw new RuntimeException("Role mismatch");
        }
        user.setFullName(updatedUser.getFullName());
        user.setEmail(updatedUser.getEmail());
        user.setIdNumber(updatedUser.getIdNumber());
        user.setAddress(updatedUser.getAddress());
        user.setPhone(updatedUser.getPhone());
        // Only update password if provided
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id, User.Role requiredRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != requiredRole) {
            throw new RuntimeException("Role mismatch");
        }
        userRepository.delete(user);
    }

    @Override
    public List<User> findClientsByAgent(Long agentId) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.CLIENT && agentId.equals(u.getCreatedBy()))
                .toList();
    }

    @Override
    public User updateClientByAgent(Long clientId, User updatedUser, Long agentId) {
        User user = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        if (user.getRole() != User.Role.CLIENT || !agentId.equals(user.getCreatedBy())) {
            throw new RuntimeException("Not authorized to update this client");
        }
        user.setFullName(updatedUser.getFullName());
        user.setEmail(updatedUser.getEmail());
        user.setIdNumber(updatedUser.getIdNumber());
        user.setAddress(updatedUser.getAddress());
        user.setPhone(updatedUser.getPhone());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public void deleteClientByAgent(Long clientId, Long agentId) {
        User user = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        if (user.getRole() != User.Role.CLIENT || !agentId.equals(user.getCreatedBy())) {
            throw new RuntimeException("Not authorized to delete this client");
        }
        userRepository.delete(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
} 