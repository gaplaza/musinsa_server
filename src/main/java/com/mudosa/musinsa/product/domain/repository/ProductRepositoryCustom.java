package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.Product;

import java.util.Optional;

/**
 * 상품 상세 조회 전용 커스텀 리포지토리.
 */
public interface ProductRepositoryCustom {

    /**
     * 상세 화면에 필요한 연관을 순차적으로 로딩한다.
     */
    Optional<Product> findDetailById(Long productId);
}
