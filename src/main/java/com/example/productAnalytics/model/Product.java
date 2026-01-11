package com.example.productAnalytics.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(
        name = "product_bangalore_hyderabad",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "name"),
                @UniqueConstraint(columnNames = "product_id"),
                @UniqueConstraint(columnNames = "image")
        }
)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Double price;

    private String description;

    @Column(nullable = false)
    private String image;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;
}
