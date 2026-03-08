package com.ziminsure.insurance.service;

import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.domain.Car;
import java.util.List;
import java.util.Optional;

public interface UserService {
    User registerUser(User user);

    Optional<User> findByEmail(String email);

    boolean checkPassword(User user, String rawPassword);

    List<User> findByRole(User.Role role);

    /** Users who can be assigned clients (AGENT or SUPER_ADMIN). For SUPER_ADMIN assignment UI. */
    List<User> findAssignableUsers();

    User updateUser(Long id, User updatedUser, User.Role requiredRole);

    void deleteUser(Long id, User.Role requiredRole);

    List<User> findClientsByAgent(Long agentId);

    User updateClientByAgent(Long clientId, User updatedUser, Long agentId);

    /**
     * Update client fields and sync cars: add new, update existing, remove cars not in the list.
     */
    User updateClientWithCars(Long clientId, User updatedUser, List<Car> cars, Long agentId);

    /**
     * Sync cars for a client (add new, update existing, remove missing). No auth check; use for SUPER_ADMIN.
     */
    void syncClientCars(Long clientId, List<Car> cars);

    void deleteClientByAgent(Long clientId, Long agentId);

    Optional<User> findById(Long id);

    User registerUserWithCars(User user, List<Car> cars);

    User save(User user);

    void deleteById(Long id);

    String encodePassword(String rawPassword);

    Optional<User> findWithCarsByEmail(String email);

    Optional<User> findWithCarsById(Long id);
}