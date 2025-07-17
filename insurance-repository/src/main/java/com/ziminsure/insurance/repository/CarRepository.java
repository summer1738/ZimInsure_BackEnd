package com.ziminsure.insurance.repository;

import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByClient(User client);
    List<Car> findByClientId(Long clientId);
    Car findByRegNumber(String regNumber);
} 