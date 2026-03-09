package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.api.dto.NotificationDto;
import com.ziminsure.insurance.domain.Notification;
import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.service.NotificationService;
import com.ziminsure.insurance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserService userService;

    /**
     * Returns notifications for the current user. For CLIENT/AGENT only their own;
     * for SUPER_ADMIN all. When the viewer is not the recipient (e.g. admin/agent viewing
     * a client notification), displayMessage is set so "Your" becomes "Client X's".
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<NotificationDto> getForCurrentUser(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<Notification> list;
        if (currentUser.getRole() == User.Role.CLIENT) {
            list = notificationService.getNotificationsByClientId(currentUser.getId());
        } else if (currentUser.getRole() == User.Role.AGENT) {
            list = notificationService.getNotificationsByAgentId(currentUser.getId());
        } else {
            list = notificationService.getAllNotifications();
        }
        return toDtos(list, currentUser);
    }

    private List<NotificationDto> toDtos(List<Notification> list, User viewer) {
        List<NotificationDto> dtos = new ArrayList<>();
        for (Notification n : list) {
            NotificationDto dto = new NotificationDto();
            dto.setId(n.getId());
            dto.setMessage(n.getMessage());
            dto.setType(n.getType());
            dto.setTimestamp(n.getTimestamp());
            dto.setRead(n.isRead());
            dto.setForRole(n.getForRole());
            dto.setAgentId(n.getAgentId());
            dto.setClientId(n.getClientId());
            dto.setCarId(n.getCarId());
            // When viewer is SUPER_ADMIN or AGENT and notification was for a client, show "Client X's ..." instead of "Your ..."
            if (n.getForRole() != null && "CLIENT".equals(n.getForRole()) && n.getClientId() != null
                    && (viewer.getRole() == User.Role.SUPER_ADMIN || viewer.getRole() == User.Role.AGENT)
                    && !viewer.getId().equals(n.getClientId())) {
                String clientName = userService.findById(n.getClientId())
                        .map(u -> u.getFullName() != null ? u.getFullName() : u.getEmail())
                        .orElse("Client");
                String msg = n.getMessage();
                if (msg != null) {
                    msg = msg.replace("Your ", clientName + "'s ").replace("your ", clientName + "'s ");
                }
                dto.setDisplayMessage(msg);
            }
            dtos.add(dto);
        }
        return dtos;
    }

    @GetMapping("/role/{role}")
    public List<Notification> getByRole(@PathVariable("role") String role) {
        return notificationService.getNotificationsByRole(role);
    }

    @GetMapping("/agent/{agentId}")
    public List<Notification> getByAgent(@PathVariable("agentId") Long agentId) {
        return notificationService.getNotificationsByAgentId(agentId);
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPER_ADMIN')")
    public ResponseEntity<List<Notification>> getByClient(@PathVariable("clientId") Long clientId,
                                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() == User.Role.CLIENT) {
            // CLIENT can only fetch their own notifications
            if (!user.getId().equals(clientId)) {
                return ResponseEntity.status(403).build();
            }
        } else if (user.getRole() == User.Role.AGENT) {
            Optional<User> client = userService.findById(clientId);
            if (client.isEmpty() || !user.getId().equals(client.get().getCreatedBy())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(notificationService.getNotificationsByClientId(clientId));
    }

    @GetMapping
    public List<Notification> getAllNotifications() {
        return notificationService.getAllNotifications();
    }

    @PostMapping
    public Notification addNotification(@RequestBody Notification notification) {
        return notificationService.addNotification(
                notification.getMessage(),
                notification.getType(),
                notification.getForRole(),
                notification.getAgentId(),
                notification.getClientId(),
                notification.getCarId()
        );
    }

    @PutMapping("/read/{id}")
    public Optional<Notification> markAsRead(@PathVariable("id") Long id) {
        return notificationService.markAsRead(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Optional<Notification> opt = notificationService.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Notification n = opt.get();
        boolean allowed = false;
        if (currentUser.getRole() == User.Role.CLIENT) {
            allowed = n.getClientId() != null && n.getClientId().equals(currentUser.getId());
        } else if (currentUser.getRole() == User.Role.AGENT) {
            allowed = n.getAgentId() != null && n.getAgentId().equals(currentUser.getId());
        } else {
            allowed = true; // SUPER_ADMIN
        }
        if (!allowed) {
            return ResponseEntity.status(403).build();
        }
        notificationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/readAll")
    @PreAuthorize("isAuthenticated()")
    public void markAllAsReadForCurrentUser(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        notificationService.markAllAsReadForUser(currentUser);
    }

    @PutMapping("/readAll")
    public void markAllAsRead(@RequestParam(name = "forRole", required = false) String forRole,
                              @RequestParam(name = "agentId", required = false) Long agentId,
                              @RequestParam(name = "clientId", required = false) Long clientId) {
        notificationService.markAllAsReadByUser(forRole, agentId, clientId);
    }
} 