package com.ziminsure.insurance.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ziminsure.insurance.domain.Car;
import com.ziminsure.insurance.domain.User;
import java.util.List;

public class UserProfileDTO {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String idNumber;
    private String status;
    private User.Role role;
    private List<Car> cars;

    public UserProfileDTO(Long id, String email, String fullName, String phone, String address,
                          String idNumber, String status, User.Role role, List<Car> cars) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.idNumber = idNumber;
        this.status = status;
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

    @JsonProperty("full_name")
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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