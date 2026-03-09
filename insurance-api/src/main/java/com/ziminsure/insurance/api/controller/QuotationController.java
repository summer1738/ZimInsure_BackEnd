package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.api.dto.QuotationRequestDTO;
import com.ziminsure.insurance.api.dto.ClientQuotationActionDTO;
import com.ziminsure.insurance.domain.Quotation;
import com.ziminsure.insurance.domain.QuotationResponse;
import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.Policy;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.repository.QuotationRepository;
import com.ziminsure.insurance.repository.CarRepository;
import com.ziminsure.insurance.repository.PolicyRepository;
import com.ziminsure.insurance.service.UserService;
import com.ziminsure.insurance.service.NotificationService;
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
    private PolicyRepository policyRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private NotificationService notificationService;

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
            q.getAgent() != null ? q.getAgent().getId() : null,
            q.getInsuranceCompany(),
            q.getPolicyId(),
            q.getClientProposedAmount(),
            q.getClientComment()
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
        quotation.setPolicyId(dto.getPolicyId());
        quotation.setInsuranceCompany(dto.getInsuranceCompany());
        quotation.setPolicyId(dto.getPolicyId());
        quotation.setInsuranceCompany(dto.getInsuranceCompany());
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

        // Notify client about new quotation, if a client is linked
        if (saved.getClient() != null) {
            User clientUser = saved.getClient();
            String reg = saved.getCar() != null && saved.getCar().getRegNumber() != null
                    ? saved.getCar().getRegNumber()
                    : "your car";
            String msg = String.format(
                    "You have a new quotation %s for %s from %s. Amount: %.2f.",
                    saved.getQuotationNumber(),
                    reg,
                    saved.getInsuranceCompany() != null ? saved.getInsuranceCompany() : "your insurer",
                    saved.getAmount() != null ? saved.getAmount() : 0.0
            );
            notificationService.addNotification(
                    msg,
                    "info",
                    "CLIENT",
                    null,
                    clientUser.getId(),
                    saved.getCar() != null ? saved.getCar().getId() : null
            );
        }

        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * Create a quotation automatically from an existing policy for a given client/car.
     * The quotation will copy policy type, premium and insurance company, and start as PENDING for the client.
     */
    @PostMapping("/from-policy/{policyId}")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> createFromPolicy(@PathVariable("policyId") Long policyId, Authentication authentication) {
        Optional<Policy> policyOpt = policyRepository.findById(policyId);
        if (policyOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Policy not found");
        }
        Policy policy = policyOpt.get();
        if (policy.getClient() == null || policy.getCar() == null) {
            return ResponseEntity.badRequest().body("Policy must be linked to a client and car");
        }
        User user = (User) authentication.getPrincipal();
        // Agents can only generate quotations for their own clients
        if (user.getRole() == User.Role.AGENT) {
            if (policy.getClient().getCreatedBy() == null || !user.getId().equals(policy.getClient().getCreatedBy())) {
                return ResponseEntity.status(403).body("Not allowed to generate quotation for this client");
            }
        }
        // Build a quotation from the policy
        Quotation quotation = new Quotation();
        quotation.setQuotationNumber("Q-" + policy.getPolicyNumber());
        quotation.setPolicyType(policy.getType());
        quotation.setStatus("PENDING");
        quotation.setAmount(policy.getPremium());
        quotation.setCreatedDate(LocalDate.now());
        quotation.setClient(policy.getClient());
        quotation.setCar(policy.getCar());
        quotation.setPolicyId(policy.getId());
        quotation.setInsuranceCompany(policy.getInsuranceCompany());
        if (user.getRole() == User.Role.AGENT) {
            quotation.setAgent(user);
        } else if (user.getRole() == User.Role.SUPER_ADMIN && policy.getClient().getCreatedBy() != null) {
            // SUPER_ADMIN: try set agent to the client's assigned agent for convenience
            userService.findById(policy.getClient().getCreatedBy()).ifPresent(quotation::setAgent);
        }
        Quotation saved = quotationRepository.save(quotation);

        // Notify client about new quotation from policy
        if (saved.getClient() != null) {
            User clientUser = saved.getClient();
            String reg = saved.getCar() != null && saved.getCar().getRegNumber() != null
                    ? saved.getCar().getRegNumber()
                    : "your car";
            String msg = String.format(
                    "A quotation %s has been generated from your policy for %s. Amount: %.2f.",
                    saved.getQuotationNumber(),
                    reg,
                    saved.getAmount() != null ? saved.getAmount() : 0.0
            );
            notificationService.addNotification(
                    msg,
                    "info",
                    "CLIENT",
                    null,
                    clientUser.getId(),
                    saved.getCar() != null ? saved.getCar().getId() : null
            );
        }

        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * Client responds to a quotation: ACCEPT, DECLINE, or NEGOTIATE.
     */
    @PostMapping("/{id}/client-action")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> clientAction(@PathVariable("id") Long id, @RequestBody ClientQuotationActionDTO dto, Authentication authentication) {
        Optional<Quotation> quotationOpt = quotationRepository.findById(id);
        if (quotationOpt.isEmpty()) return ResponseEntity.notFound().build();
        Quotation quotation = quotationOpt.get();
        User client = (User) authentication.getPrincipal();
        if (quotation.getClient() == null || !quotation.getClient().getId().equals(client.getId())) {
            return ResponseEntity.status(403).body("Not allowed to act on this quotation");
        }
        String action = dto.getAction() != null ? dto.getAction().toUpperCase() : "";
        switch (action) {
            case "ACCEPT" -> quotation.setStatus("ACCEPTED");
            case "DECLINE" -> quotation.setStatus("DECLINED");
            case "NEGOTIATE" -> {
                quotation.setStatus("NEGOTIATION_REQUESTED");
                quotation.setClientProposedAmount(dto.getProposedAmount());
                quotation.setClientComment(dto.getComment());
            }
            default -> {
                return ResponseEntity.badRequest().body("Unsupported action. Use ACCEPT, DECLINE or NEGOTIATE.");
            }
        }
        Quotation saved = quotationRepository.save(quotation);

        // Notify client and agent about the action
        User clientUser = quotation.getClient();
        User agentUser = quotation.getAgent();
        String qNum = quotation.getQuotationNumber();
        String reg = quotation.getCar() != null && quotation.getCar().getRegNumber() != null
                ? quotation.getCar().getRegNumber()
                : "your car";

        switch (action) {
            case "ACCEPT" -> {
                if (clientUser != null) {
                    notificationService.addNotification(
                            String.format("You accepted quotation %s for %s.", qNum, reg),
                            "success",
                            "CLIENT",
                            null,
                            clientUser.getId(),
                            quotation.getCar() != null ? quotation.getCar().getId() : null
                    );
                }
                if (agentUser == null && clientUser != null && clientUser.getCreatedBy() != null) {
                    agentUser = userService.findById(clientUser.getCreatedBy()).orElse(null);
                }
                if (agentUser != null) {
                    notificationService.addNotification(
                            String.format("Client %s accepted quotation %s for %s.",
                                    clientUser != null ? clientUser.getFullName() : "client",
                                    qNum,
                                    reg),
                            "info",
                            "AGENT",
                            agentUser.getId(),
                            clientUser != null ? clientUser.getId() : null,
                            quotation.getCar() != null ? quotation.getCar().getId() : null
                    );
                }
            }
            case "DECLINE" -> {
                if (clientUser != null) {
                    notificationService.addNotification(
                            String.format("You declined quotation %s for %s.", qNum, reg),
                            "info",
                            "CLIENT",
                            null,
                            clientUser.getId(),
                            quotation.getCar() != null ? quotation.getCar().getId() : null
                    );
                }
                if (agentUser == null && clientUser != null && clientUser.getCreatedBy() != null) {
                    agentUser = userService.findById(clientUser.getCreatedBy()).orElse(null);
                }
                if (agentUser != null) {
                    notificationService.addNotification(
                            String.format("Client %s declined quotation %s for %s.",
                                    clientUser != null ? clientUser.getFullName() : "client",
                                    qNum,
                                    reg),
                            "warning",
                            "AGENT",
                            agentUser.getId(),
                            clientUser != null ? clientUser.getId() : null,
                            quotation.getCar() != null ? quotation.getCar().getId() : null
                    );
                }
            }
            case "NEGOTIATE" -> {
                if (clientUser != null) {
                    notificationService.addNotification(
                            String.format("You requested a better price for quotation %s for %s.", qNum, reg),
                            "info",
                            "CLIENT",
                            null,
                            clientUser.getId(),
                            quotation.getCar() != null ? quotation.getCar().getId() : null
                    );
                }
                if (agentUser == null && clientUser != null && clientUser.getCreatedBy() != null) {
                    agentUser = userService.findById(clientUser.getCreatedBy()).orElse(null);
                }
                if (agentUser != null) {
                    String msg = String.format("Client %s requested a better price for quotation %s (proposed: %.2f, comment: %s).",
                            clientUser != null ? clientUser.getFullName() : "client",
                            qNum,
                            quotation.getClientProposedAmount() != null ? quotation.getClientProposedAmount() : 0.0,
                            quotation.getClientComment() != null ? quotation.getClientComment() : ""
                    );
                    notificationService.addNotification(
                            msg,
                            "info",
                            "AGENT",
                            agentUser.getId(),
                            clientUser != null ? clientUser.getId() : null,
                            quotation.getCar() != null ? quotation.getCar().getId() : null
                    );
                }
            }
        }

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
        if (dto.getPolicyId() != null) existing.setPolicyId(dto.getPolicyId());
        if (dto.getInsuranceCompany() != null) existing.setInsuranceCompany(dto.getInsuranceCompany());
        if (dto.getClientProposedAmount() != null) existing.setClientProposedAmount(dto.getClientProposedAmount());
        if (dto.getClientComment() != null) existing.setClientComment(dto.getClientComment());
        Quotation saved = quotationRepository.save(existing);

        // Notify client that their quotation has been updated
        if (saved.getClient() != null) {
            User clientUser = saved.getClient();
            String reg = saved.getCar() != null && saved.getCar().getRegNumber() != null
                    ? saved.getCar().getRegNumber()
                    : "your car";
            String msg = String.format("Your quotation %s for %s has been updated. Please review it.",
                    saved.getQuotationNumber(),
                    reg);
            notificationService.addNotification(
                    msg,
                    "info",
                    "CLIENT",
                    null,
                    clientUser.getId(),
                    saved.getCar() != null ? saved.getCar().getId() : null
            );
        }

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