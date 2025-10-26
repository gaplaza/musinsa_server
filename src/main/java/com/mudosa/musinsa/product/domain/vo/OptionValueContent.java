package com.mudosa.musinsa.product.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class OptionValueContent {
    
    @Column(name = "option_value", nullable = false, length = 50)
    private String value;
    
    private OptionValueContent(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("옵션 값은 필수입니다.");
        }
        if (value.trim().length() > 50) {
            throw new IllegalArgumentException("옵션 값은 50자를 초과할 수 없습니다.");
        }
        this.value = value.trim();
    }
    
    public static OptionValueContent of(String value) {
        return new OptionValueContent(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}