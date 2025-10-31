package com.mudosa.musinsa.product.domain.vo;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductGenderType {

    @Enumerated(EnumType.STRING)
    private Type value;

    public ProductGenderType(Type value) {
        if (value == null) {
            throw new IllegalArgumentException("성별 타입은 null일 수 없습니다.");
        }
        this.value = value;
    }

    public enum Type {
        MEN,    // 남성용
        WOMEN,  // 여성용
        ALL     // 공용
    }

    @Override
    public String toString() {
        return value.name();
    }
}
