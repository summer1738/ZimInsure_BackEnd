package com.ziminsure.insurance.api.dto;

import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.domain.Car;
import java.util.List;

public class ClientWithCarsDTO {
    private User client;
    private List<Car> cars;

    public User getClient() {
        return client;
    }
    public void setClient(User client) {
        this.client = client;
    }
    public List<Car> getCars() {
        return cars;
    }
    public void setCars(List<Car> cars) {
        this.cars = cars;
    }
} 