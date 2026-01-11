package com.example.productAnalytics.repository;

import com.example.productAnalytics.model.ProductView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductViewRepository extends JpaRepository<ProductView, UUID> {
    Optional<ProductView> findByProductIdAndUserIp(String productId, String userIp);

    @Query("""
            SELECT pv.productId as productId, SUM(pv.viewCount) as viewCount
            FROM ProductView pv
            GROUP BY pv.productId
            ORDER BY MAX(pv.lastUpdated) DESC
            """)
    List<Object[]> findRecentProducts(Pageable pageable);

    @Query("""
            SELECT pv.productId as productId, SUM(pv.viewCount) as viewCount
            FROM ProductView pv
            GROUP BY pv.productId
            ORDER BY SUM(pv.viewCount) DESC
            """)
    List<Object[]> findFrequentProducts(Pageable pageable);
}
