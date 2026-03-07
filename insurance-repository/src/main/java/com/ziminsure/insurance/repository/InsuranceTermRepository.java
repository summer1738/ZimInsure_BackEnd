package com.ziminsure.insurance.repository;

import com.ziminsure.insurance.domain.InsuranceTerm;
import com.ziminsure.insurance.domain.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InsuranceTermRepository extends JpaRepository<InsuranceTerm, Long> {
    List<InsuranceTerm> findByCar(Car car);
    List<InsuranceTerm> findByCarId(Long carId);
    List<InsuranceTerm> findByEndDateBetween(LocalDate start, LocalDate end);
} 