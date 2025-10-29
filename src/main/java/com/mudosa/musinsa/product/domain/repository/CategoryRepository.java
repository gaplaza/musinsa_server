package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 상품 카테고리 조회용 리포지토리.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
