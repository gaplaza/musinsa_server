package com.mudosa.musinsa.product.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 옵션값 엔티티
 * 특정 옵션명에 속하는 값
 * 예: OptionName("사이즈") -> OptionValue("270", "275", "280")
 */
@Entity
@Table(
    name = "option_value",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uniq_option_value_name_val",
            columnNames = {"option_name_id", "option_value"}
        )
    },
    indexes = {
        @Index(name = "idx_optval_option_name_id", columnList = "option_name_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionValue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_value_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_name_id", nullable = false)
    private OptionName optionName;
    
    @Column(name = "option_value", nullable = false, length = 50)
    private String optionValue;
    
    /**
     * 옵션값 생성
     */
    public static OptionValue create(OptionName optionName, String optionValue) {
        OptionValue entity = new OptionValue();
        entity.optionName = optionName;
        entity.optionValue = optionValue;
        return entity;
    }
    
    /**
     * 옵션값 수정
     */
    public void updateOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }
}
