package com.example.productAnalytics.repository;

import com.example.productAnalytics.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Query("SELECT p.productId, p.name FROM Product p")
    List<Object[]> findAllProductIdAndName();

    @Query("SELECT p FROM Product p WHERE p.productId = :productId")
    Optional<Product> findByProductId(String productId);
}
