package ru.liga.carsetl.domain;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class Suv {
    private String vin;
    private Integer milage;
    private String location;
    private Integer price; // kUsd

    public Suv(Car car) {
        this(car.getVin(), car.getMilage(), car.getLocation(), car.getPrice());
    }
}


