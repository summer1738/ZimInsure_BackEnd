package com.ziminsure.insurance.api.scheduler;

import com.ziminsure.insurance.domain.InsuranceTerm;
import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.repository.InsuranceTermRepository;
import com.ziminsure.insurance.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class InsuranceNotificationScheduler {

    private final InsuranceTermRepository insuranceTermRepository;
    private final NotificationService notificationService;

    public InsuranceNotificationScheduler(InsuranceTermRepository insuranceTermRepository,
                                          NotificationService notificationService) {
        this.insuranceTermRepository = insuranceTermRepository;
        this.notificationService = notificationService;
    }

    /**
     * Once per day, notify clients (and their agents) whose insurance will expire in exactly 30 days.
     * This runs at 08:00 server time.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void notifyClientsOfUpcomingExpiry() {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(30);

        List<InsuranceTerm> expiringIn30Days = insuranceTermRepository.findByEndDateBetween(targetDate, targetDate);

        for (InsuranceTerm term : expiringIn30Days) {
            Car car = term.getCar();
            if (car == null || car.getClient() == null) {
                continue;
            }
            User client = car.getClient();
            String reg = car.getRegNumber() != null ? car.getRegNumber() : "your car";
            String message = String.format(
                    "Your insurance for %s will expire on %s. Please contact your agent to renew.",
                    reg,
                    term.getEndDate()
            );
            notificationService.addNotification(
                    message,
                    "warning",
                    "CLIENT",
                    null,
                    client.getId(),
                    car.getId()
            );

            // Also notify assigned agent if any
            if (client.getCreatedBy() != null) {
                Long agentId = client.getCreatedBy();
                String agentMsg = String.format(
                        "Insurance for client %s, car %s, will expire on %s.",
                        client.getFullName() != null ? client.getFullName() : client.getEmail(),
                        reg,
                        term.getEndDate()
                );
                notificationService.addNotification(
                        agentMsg,
                        "warning",
                        "AGENT",
                        agentId,
                        client.getId(),
                        car.getId()
                );
            }
        }
    }

    /**
     * Once per day, notify clients (and their agents) whose insurance expires today.
     * This runs at 08:30 server time.
     */
    @Scheduled(cron = "0 30 8 * * *")
    public void notifyClientsOfExpiredInsurance() {
        LocalDate today = LocalDate.now();

        List<InsuranceTerm> expiringToday = insuranceTermRepository.findByEndDateBetween(today, today);

        for (InsuranceTerm term : expiringToday) {
            Car car = term.getCar();
            if (car == null || car.getClient() == null) {
                continue;
            }
            User client = car.getClient();
            String reg = car.getRegNumber() != null ? car.getRegNumber() : "your car";

            String clientMsg = String.format(
                    "Your insurance for %s expires today (%s). You will not be covered after this date.",
                    reg,
                    term.getEndDate()
            );
            notificationService.addNotification(
                    clientMsg,
                    "error",
                    "CLIENT",
                    null,
                    client.getId(),
                    car.getId()
            );

            if (client.getCreatedBy() != null) {
                Long agentId = client.getCreatedBy();
                String agentMsg = String.format(
                        "Insurance for client %s, car %s, expires today (%s).",
                        client.getFullName() != null ? client.getFullName() : client.getEmail(),
                        reg,
                        term.getEndDate()
                );
                notificationService.addNotification(
                        agentMsg,
                        "warning",
                        "AGENT",
                        agentId,
                        client.getId(),
                        car.getId()
                );
            }
        }
    }
}

