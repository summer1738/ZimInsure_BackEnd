package com.ziminsure.insurance.repository;

import com.ziminsure.insurance.domain.Quotation;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.domain.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByClient(User client);
    List<Quotation> findByAgent(User agent);
    List<Quotation> findByCar(Car car);
    List<Quotation> findByStatus(String status);
    Quotation findByQuotationNumber(String quotationNumber);
} 