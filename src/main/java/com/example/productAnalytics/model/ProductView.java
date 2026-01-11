package com.example.productAnalytics.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(
        name = "product_view_bangalore_hyderabad",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"product_id", "user_ip"})
        }
)
public class ProductView {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String userIp;

    @Column(nullable = false)
    private Integer viewCount;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime lastUpdated;
}
