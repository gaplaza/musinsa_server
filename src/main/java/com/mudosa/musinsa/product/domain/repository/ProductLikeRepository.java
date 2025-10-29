package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.Product;
import com.mudosa.musinsa.product.domain.model.ProductLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 상품 좋아요 토글과 집계를 위한 리포지토리.
 */
@Repository
public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {

    Optional<ProductLike> findByProductAndUserId(Product product, Long userId);

    long countByProduct(Product product);
}
