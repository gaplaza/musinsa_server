package com.mudosa.musinsa.product.domain.model;

import com.mudosa.musinsa.common.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 애그리거트 루트
 * - ProductOption과 별도 애그리거트 (재고 관리)
 */
@Entity
@Table(name = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long id;
    
    @Column(name = "product_option_id", nullable = false)
    private Long productOptionId; // ProductOption 애그리거트 참조 (ID만)
    
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;
    
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;
    
    /**
     * 재고 생성
     */
    public static Inventory create(Long productOptionId, int stockQuantity) {
        Inventory inventory = new Inventory();
        inventory.productOptionId = productOptionId;
        inventory.stockQuantity = stockQuantity;
        inventory.isAvailable = true;
        return inventory;
    }
    
    /**
     * 재고 증가
     */
    public void increase(int quantity) {
        this.stockQuantity += quantity;
        if (this.stockQuantity > 0) {
            this.isAvailable = true;
        }
    }
    
    /**
     * 재고 감소
     */
    public void decrease(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        this.stockQuantity -= quantity;
        if (this.stockQuantity == 0) {
            this.isAvailable = false;
        }
    }
    
    /**
     * 재고 가용성 확인
     */
    public boolean canOrder(int quantity) {
        return isAvailable && stockQuantity >= quantity;
    }
    
    /**
     * 품절 처리
     */
    public void markAsUnavailable() {
        this.isAvailable = false;
    }
    
    /**
     * 판매 가능 처리
     */
    public void markAsAvailable() {
        this.isAvailable = true;
    }
}
