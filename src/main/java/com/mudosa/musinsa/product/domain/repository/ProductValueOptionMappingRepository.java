package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.ProductValueOptionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 상품옵션-옵션값 매핑 Repository
 */
@Repository
public interface ProductValueOptionMappingRepository extends 
    JpaRepository<ProductValueOptionMapping, ProductValueOptionMapping.ProductValueOptionMappingId> {
    
    /**
     * 특정 상품 옵션의 모든 옵션값 매핑 조회
     */
    @Query("SELECT m FROM ProductValueOptionMapping m " +
           "JOIN FETCH m.optionValue ov " +
           "JOIN FETCH ov.optionName " +
           "WHERE m.productOptionId = :productOptionId")
    List<ProductValueOptionMapping> findByProductOptionIdWithFetch(
        @Param("productOptionId") Long productOptionId
    );
    
    /**
     * 특정 옵션값이 사용된 모든 상품 옵션 ID 조회
     */
    @Query("SELECT m.productOptionId FROM ProductValueOptionMapping m " +
           "WHERE m.optionValueId = :optionValueId")
    List<Long> findProductOptionIdsByOptionValueId(@Param("optionValueId") Long optionValueId);
    
    /**
     * 특정 상품의 모든 옵션과 옵션값 매핑 조회
     */
    @Query("SELECT m FROM ProductValueOptionMapping m " +
           "JOIN FETCH m.productOption po " +
           "JOIN FETCH m.optionValue ov " +
           "JOIN FETCH ov.optionName " +
           "WHERE po.product.id = :productId")
    List<ProductValueOptionMapping> findByProductIdWithFetch(@Param("productId") Long productId);
}
