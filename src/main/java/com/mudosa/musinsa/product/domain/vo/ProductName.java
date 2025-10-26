package com.mudosa.musinsa.product.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 상품명 Value Object
 * DDL: VARCHAR(100) NOT NULL
 */
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductName {

    @Column(name = "product_name", nullable = false, length = 100)
    private String value;

    private ProductName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("상품명은 100자를 초과할 수 없습니다.");
        }
        this.value = value.trim();
    }

    public static ProductName of(String value) {
        return new ProductName(value);
    }

    public String getValue() {
        return value;
    }
}