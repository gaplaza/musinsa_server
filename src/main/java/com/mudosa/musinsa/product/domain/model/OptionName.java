package com.mudosa.musinsa.product.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 옵션명 엔티티
 * 예: "사이즈", "색상", "용량" 등
 */
@Entity
@Table(name = "option_name")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionName {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_name_id")
    private Long id;
    
    @Column(name = "option_name", nullable = false, length = 50, unique = true)
    private String optionName;
    
    /**
     * 옵션명 생성
     */
    public static OptionName create(String optionName) {
        OptionName entity = new OptionName();
        entity.optionName = optionName;
        return entity;
    }
    
    /**
     * 옵션명 수정
     */
    public void updateOptionName(String optionName) {
        this.optionName = optionName;
    }
}
