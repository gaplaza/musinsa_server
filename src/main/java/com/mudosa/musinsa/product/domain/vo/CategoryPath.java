package com.mudosa.musinsa.product.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 카테고리 경로 Value Object
 * DDL: VARCHAR(255) NOT NULL (비정규화된 필드)
 */
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryPath {

    @Column(name = "category_path", nullable = false, length = 255)
    private String value;

    private CategoryPath(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("카테고리 경로는 필수입니다.");
        }
        if (value.length() > 255) {
            throw new IllegalArgumentException("카테고리 경로는 255자를 초과할 수 없습니다.");
        }
        this.value = value.trim();
    }

    public static CategoryPath of(String value) {
        return new CategoryPath(value);
    }

    public String getValue() {
        return value;
    }
}