package com.ziminsure.insurance.service.impl;

import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.repository.CarRepository;
import com.ziminsure.insurance.repository.UserRepository;
import com.ziminsure.insurance.service.UserService;
import com.ziminsure.insurance.service.InsuranceTermService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private final PasswordEncoder passwordEncoder;
    private final InsuranceTermService insuranceTermService;

    public UserServiceImpl(UserRepository userRepository, CarRepository carRepository,
            PasswordEncoder passwordEncoder, InsuranceTermService insuranceTermService) {
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.passwordEncoder = passwordEncoder;
        this.insuranceTermService = insuranceTermService;
    }

    @Override
    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User registerUserWithCars(User user, List<Car> cars) {
        System.out.println("=== REGISTER USER WITH CARS DEBUG ===");
        System.out.println("User to save: " + user);
        System.out.println("Cars to save: " + cars);
        
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            System.out.println("Password encoded successfully");
            
            System.out.println("About to save user...");
            User savedUser = userRepository.save(user);
            System.out.println("User saved successfully with ID: " + savedUser.getId());
            
            for (Car car : cars) {
                System.out.println("Processing car: " + car.getRegNumber());
                
                if (carRepository.findByRegNumber(car.getRegNumber()) != null) {
                    System.out.println("ERROR: Car with registration number " + car.getRegNumber() + " already exists");
                    throw new RuntimeException("Car with registration number " + car.getRegNumber() + " already exists");
                }
                
                car.setClient(savedUser);
                System.out.println("About to save car: " + car.getRegNumber());
                Car savedCar = carRepository.save(car);
                System.out.println("Car saved successfully with ID: " + savedCar.getId());

                logger.info("Car saved with ID: {}, now creating default insurance term", savedCar.getId());

                // Create default insurance term for the car
                try {
                    insuranceTermService.createDefaultInsuranceTerm(savedCar);
                    logger.info("Default insurance term created successfully for car ID: {}", savedCar.getId());
                } catch (Exception e) {
                    logger.error("Failed to create default insurance term for car ID: {}", savedCar.getId(), e);
                }
            }
            
            System.out.println("All cars processed successfully");
            return savedUser;
        } catch (Exception e) {
            System.out.println("ERROR in registerUserWithCars: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
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

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public Optional<User> findWithCarsByEmail(String email) {
        return userRepository.findWithCarsByEmail(email);
    }
}