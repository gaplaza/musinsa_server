package com.mudosa.musinsa.product.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_category")
public class ProductCategory {
    
    @EmbeddedId
    private ProductCategoryId id;
    
    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    
    @MapsId("categoryId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @Builder
    public ProductCategory(Product product, Category category) {
        this.product = product;
        this.category = category;
        this.id = new ProductCategoryId(
            product.getProductId(),
            category.getCategoryId()
        );
    }
    
    // 도메인 로직: 상품-카테고리 매핑 정보 수정
    public void modify(Product product, Category category) {
        if (product != null) {
            this.product = product;
            this.id.productId = product.getProductId();
        }
        if (category != null) {
            this.category = category;
            this.id.categoryId = category.getCategoryId();
        }
    }
    
    // 도메인 로직: 특정 상품의 카테고리 여부 확인
    public boolean belongsToProduct(Product product) {
        return this.product != null && this.product.equals(product);
    }
    
    // 도메인 로직: 특정 카테고리의 상품 여부 확인
    public boolean belongsToCategory(Category category) {
        return this.category != null && this.category.equals(category);
    }
    
    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Getter
    public static class ProductCategoryId implements Serializable {
        
        @Column(name = "product_id")
        private Long productId;
        
        @Column(name = "category_id")
        private Long categoryId;
        
        public ProductCategoryId(Long productId, Long categoryId) {
            this.productId = productId;
            this.categoryId = categoryId;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductCategoryId that = (ProductCategoryId) o;
            return Objects.equals(productId, that.productId) &&
                   Objects.equals(categoryId, that.categoryId);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(productId, categoryId);
        }
    }
}