package com.ziminsure.insurance.service.impl;

import com.ziminsure.insurance.domain.InsuranceTerm;
import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.repository.InsuranceTermRepository;
import com.ziminsure.insurance.service.InsuranceTermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class InsuranceTermServiceImpl implements InsuranceTermService {
    
    private static final Logger logger = LoggerFactory.getLogger(InsuranceTermServiceImpl.class);
    private final InsuranceTermRepository insuranceTermRepository;
    
    @Autowired
    public InsuranceTermServiceImpl(InsuranceTermRepository insuranceTermRepository) {
        this.insuranceTermRepository = insuranceTermRepository;
    }
    
    @Override
    public InsuranceTerm createDefaultInsuranceTerm(Car car) {
        logger.info("Creating default insurance term for car ID: {}, RegNumber: {}", car.getId(), car.getRegNumber());
        
        InsuranceTerm defaultTerm = new InsuranceTerm();
        defaultTerm.setCar(car);
        defaultTerm.setStartDate(LocalDate.now());
        defaultTerm.setEndDate(LocalDate.now().plusMonths(4)); // 4-month default term
        defaultTerm.setTermCount(1);
        
        InsuranceTerm saved = insuranceTermRepository.save(defaultTerm);
        logger.info("Default insurance term created successfully with ID: {}", saved.getId());
        
        return saved;
    }
    
    @Override
    public List<InsuranceTerm> findByCar(Car car) {
        return insuranceTermRepository.findByCar(car);
    }
    
    @Override
    public Optional<InsuranceTerm> findCurrentByCar(Car car) {
        List<InsuranceTerm> terms = insuranceTermRepository.findByCar(car);
        LocalDate today = LocalDate.now();
        
        return terms.stream()
                .filter(term -> !term.getEndDate().isBefore(today))
                .findFirst();
    }
    
    @Override
    public boolean isCarInsured(Car car) {
        return findCurrentByCar(car).isPresent();
    }
    
    @Override
    public InsuranceTerm save(InsuranceTerm insuranceTerm) {
        return insuranceTermRepository.save(insuranceTerm);
    }
} 