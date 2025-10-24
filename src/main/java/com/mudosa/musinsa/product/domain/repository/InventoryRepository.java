package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Inventory Repository
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    Optional<Inventory> findByProductOptionId(Long productOptionId);
    
    boolean existsByProductOptionId(Long productOptionId);
}
