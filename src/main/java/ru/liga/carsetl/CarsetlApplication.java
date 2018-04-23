package ru.liga.carsetl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CarsetlApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarsetlApplication.class, args);
    }
}
