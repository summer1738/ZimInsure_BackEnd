package com.ziminsure.insurance.service;

import com.ziminsure.insurance.domain.InsuranceTerm;
import com.ziminsure.insurance.domain.Car;
import java.util.List;
import java.util.Optional;

public interface InsuranceTermService {
    InsuranceTerm createDefaultInsuranceTerm(Car car);
    List<InsuranceTerm> findByCar(Car car);
    Optional<InsuranceTerm> findCurrentByCar(Car car);
    boolean isCarInsured(Car car);
    InsuranceTerm save(InsuranceTerm insuranceTerm);
} 