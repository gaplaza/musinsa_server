package com.mudosa.musinsa.product.domain.model;

/**
 * 상품 성별 타입 Enum    
 * DDL: ENUM('MEN','WOMEN','ALL')
 */
public enum GenderType {
    MEN("남성"),
    WOMEN("여성"), 
    ALL("전체");

    private final String displayName;

    GenderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}