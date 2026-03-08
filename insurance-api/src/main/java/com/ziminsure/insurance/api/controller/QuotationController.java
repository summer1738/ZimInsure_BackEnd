package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.api.dto.QuotationRequestDTO;
import com.ziminsure.insurance.domain.Quotation;
import com.ziminsure.insurance.domain.QuotationResponse;
import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.repository.QuotationRepository;
import com.ziminsure.insurance.repository.CarRepository;
import com.ziminsure.insurance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/quotations")
public class QuotationController {
    @Autowired
    private QuotationRepository quotationRepository;
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private UserService userService;

    // List quotations
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<List<QuotationResponse>> listQuotations(@RequestParam(name = "clientId", required = false) Long clientId, @RequestParam(name = "agentId", required = false) Long agentId, @RequestParam(name = "carId", required = false) Long carId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Quotation> list;
        if (user.getRole() == User.Role.CLIENT) {
            list = quotationRepository.findByClient(user);
        } else if (user.getRole() == User.Role.AGENT) {
            if (clientId != null) {
                Optional<User> client = userService.findById(clientId);
                if (client.isPresent() && java.util.Objects.equals(client.get().getCreatedBy(), user.getId())) {
                    list = quotationRepository.findByClient(client.get());
                } else {
                    return ResponseEntity.status(403).build();
                }
            } else {
                // Agent without clientId: return all quotations for their assigned clients (e.g. dashboard)
                List<User> agentClients = userService.findClientsByAgent(user.getId());
                list = agentClients.stream()
                    .flatMap(c -> quotationRepository.findByClient(c).stream())
                    .toList();
            }
        } else if (user.getRole() == User.Role.SUPER_ADMIN) {
            if (clientId != null) {
                Optional<User> client = userService.findById(clientId);
                list = client.isPresent() ? quotationRepository.findByClient(client.get()) : List.of();
            } else if (agentId != null) {
                Optional<User> agent = userService.findById(agentId);
                list = agent.isPresent() ? quotationRepository.findByAgent(agent.get()) : List.of();
            } else if (carId != null) {
                Optional<Car> car = carRepository.findById(carId);
                list = car.isPresent() ? quotationRepository.findByCar(car.get()) : List.of();
            } else {
                list = quotationRepository.findAll();
            }
        } else {
            return ResponseEntity.status(403).build();
        }
        List<QuotationResponse> response = list.stream().map(QuotationController::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private static QuotationResponse toResponse(Quotation q) {
        return new QuotationResponse(
            q.getId(),
            q.getQuotationNumber(),
            q.getPolicyType(),
            q.getStatus(),
            q.getAmount(),
            q.getCreatedDate(),
            q.getClient() != null ? q.getClient().getId() : null,
            q.getClient() != null ? q.getClient().getFullName() : null,
            q.getCar() != null ? q.getCar().getId() : null,
            q.getCar() != null ? q.getCar().getRegNumber() : null,
            q.getAgent() != null ? q.getAgent().getId() : null
        );
    }

    // Create quotation
    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> createQuotation(@RequestBody QuotationRequestDTO dto, Authentication authentication) {
        if (dto.getCarId() == null) {
            return ResponseEntity.badRequest().body("Car is required");
        }
        Optional<Car> car = carRepository.findById(dto.getCarId());
        if (car.isEmpty()) return ResponseEntity.badRequest().body("Car not found");
        User user = (User) authentication.getPrincipal();
        Quotation quotation = new Quotation();
        quotation.setQuotationNumber(dto.getQuotationNumber());
        quotation.setPolicyType(dto.getPolicyType());
        quotation.setStatus(dto.getStatus());
        quotation.setAmount(dto.getAmount());
        quotation.setCreatedDate(dto.getCreatedDate() != null ? dto.getCreatedDate() : LocalDate.now());
        quotation.setCar(car.get());
        if (user.getRole() == User.Role.CLIENT) {
            quotation.setClient(user);
        } else if (user.getRole() == User.Role.AGENT) {
            if (dto.getClientId() == null) {
                return ResponseEntity.badRequest().body("Client is required");
            }
            Optional<User> client = userService.findById(dto.getClientId());
            if (client.isEmpty() || !java.util.Objects.equals(client.get().getCreatedBy(), user.getId())) {
                return ResponseEntity.status(403).body("Not allowed to create quotation for this client");
            }
            quotation.setClient(client.get());
            quotation.setAgent(user);
        } else if (user.getRole() == User.Role.SUPER_ADMIN) {
            if (dto.getClientId() == null) {
                return ResponseEntity.badRequest().body("Client is required");
            }
            Optional<User> clientOpt = userService.findById(dto.getClientId());
            if (clientOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Client not found");
            }
            quotation.setClient(clientOpt.get());
            if (clientOpt.get().getCreatedBy() != null) {
                userService.findById(clientOpt.get().getCreatedBy()).ifPresent(quotation::setAgent);
            }
        }
        Quotation saved = quotationRepository.save(quotation);
        return ResponseEntity.ok(toResponse(saved));
    }

    // Update quotation
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateQuotation(@PathVariable("id") Long id, @RequestBody QuotationRequestDTO dto, Authentication authentication) {
        Optional<Quotation> existingOpt = quotationRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();
        Quotation existing = existingOpt.get();
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.AGENT) {
            boolean isQuotationAgent = existing.getAgent() != null && existing.getAgent().getId().equals(user.getId());
            boolean isClientAssignedAgent = existing.getClient() != null && user.getId().equals(existing.getClient().getCreatedBy());
            if (!isQuotationAgent && !isClientAssignedAgent) {
                return ResponseEntity.status(403).build();
            }
        }
        if (dto.getQuotationNumber() != null) existing.setQuotationNumber(dto.getQuotationNumber());
        if (dto.getPolicyType() != null) existing.setPolicyType(dto.getPolicyType());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getAmount() != null) existing.setAmount(dto.getAmount());
        if (dto.getCreatedDate() != null) existing.setCreatedDate(dto.getCreatedDate());
        Quotation saved = quotationRepository.save(existing);
        return ResponseEntity.ok(toResponse(saved));
    }

    // Delete quotation
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteQuotation(@PathVariable("id") Long id, Authentication authentication) {
        Optional<Quotation> quotationOpt = quotationRepository.findById(id);
        if (quotationOpt.isEmpty()) return ResponseEntity.notFound().build();
        Quotation quotation = quotationOpt.get();
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.AGENT) {
            boolean isQuotationAgent = quotation.getAgent() != null && quotation.getAgent().getId().equals(user.getId());
            boolean isClientAssignedAgent = quotation.getClient() != null && user.getId().equals(quotation.getClient().getCreatedBy());
            if (!isQuotationAgent && !isClientAssignedAgent) {
                return ResponseEntity.status(403).build();
            }
        }
        // SUPER_ADMIN can delete any
        quotationRepository.deleteById(id);
        return ResponseEntity.ok("Quotation deleted");
    }
} 