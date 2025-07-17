package com.ziminsure.insurance.repository;

import com.ziminsure.insurance.domain.Policy;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.domain.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByClient(User client);
    List<Policy> findByCar(Car car);
    List<Policy> findByStatus(String status);
    Policy findByPolicyNumber(String policyNumber);
} 