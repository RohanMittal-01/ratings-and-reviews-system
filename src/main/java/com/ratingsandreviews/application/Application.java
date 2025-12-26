package com.ratingsandreviews.application;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Entity(name = "applications")
@Data
public class Application {
    @Id
    private UUID id;
    private String name;
    private String description;
}