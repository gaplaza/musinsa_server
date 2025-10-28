package com.mudosa.musinsa.product.domain.model;

import com.mudosa.musinsa.common.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_like")
public class ProductLike extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_like_id")
    private Long productLikeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Builder
    public ProductLike(Product product, Long userId) {
        this.product = product;
        this.userId = userId;
    }
    
    // 도메인 로직: 상품 변경
    public void changeProduct(Product product) {
        if (product != null) this.product = product;
    }
    
    // 도메인 로직: 사용자 변경
    public void changeUser(Long userId) {
        if (userId != null) this.userId = userId;
    }
    
    // 도메인 로직: 특정 상품의 좋아요 여부 확인
    public boolean belongsToProduct(Product product) {
        return this.product != null && this.product.equals(product);
    }
    
    // 도메인 로직: 특정 사용자의 좋아요 여부 확인
    public boolean belongsToUser(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }
}