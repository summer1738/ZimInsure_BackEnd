package com.ziminsure.insurance.api.dto;

import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import java.util.List;

public class UserProfileDTO {
    private Long id;
    private String email;
    private String fullName;
    private User.Role role;
    private List<Car> cars;

    public UserProfileDTO(Long id, String email, String fullName, User.Role role, List<Car> cars) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.cars = cars;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public User.Role getRole() {
        return role;
    }

    public void setRole(User.Role role) {
        this.role = role;
    }

    public List<Car> getCars() {
        return cars;
    }

    public void setCars(List<Car> cars) {
        this.cars = cars;
    }
}