package com.mudosa.musinsa.product.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ProductPrice {
    
    @Column(name = "product_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;
    
    private ProductPrice(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("상품 가격은 필수입니다.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("상품 가격은 0원 이상이어야 합니다.");
        }
        if (value.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException("상품 가격은 99,999,999원을 초과할 수 없습니다.");
        }
        this.value = value.setScale(2, RoundingMode.HALF_UP);
    }
    
    public static ProductPrice of(BigDecimal value) {
        return new ProductPrice(value);
    }
    
    public static ProductPrice of(String value) {
        return new ProductPrice(new BigDecimal(value));
    }
    
    public static ProductPrice of(long value) {
        return new ProductPrice(BigDecimal.valueOf(value));
    }
    
    public BigDecimal getValue() {
        return value;
    }
    
    public boolean isGreaterThan(ProductPrice other) {
        return this.value.compareTo(other.value) > 0;
    }
    
    public boolean isLessThan(ProductPrice other) {
        return this.value.compareTo(other.value) < 0;
    }
    
    public boolean isZero() {
        return this.value.compareTo(BigDecimal.ZERO) == 0;
    }
    
    @Override
    public String toString() {
        return value.setScale(0, RoundingMode.HALF_UP) + "원";
    }
}