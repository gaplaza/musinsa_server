package com.mudosa.musinsa.product.domain.model;

import com.mudosa.musinsa.common.domain.model.BaseEntity;
import com.mudosa.musinsa.product.domain.vo.OptionValueContent;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "option_value", 
       indexes = {
           @Index(name = "idx_optval_option_name_id", columnList = "option_name_id")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uniq_option_value_name_val", columnNames = {"option_name_id", "option_value"})
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptionValue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_value_id")
    private Long optionValueId;

    @Column(name = "option_name_id", nullable = false)  // FK (OptionName)
    private Long optionNameId;

    @Embedded
    private OptionValueContent optionValue;

    // 연관관계 - OptionName 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_name_id", insertable = false, updatable = false)
    private OptionName optionName;

    @OneToMany(mappedBy = "optionValue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductValueOptionMapping> productValueOptionMappings = new ArrayList<>();

    // 생성 메서드
    public static OptionValue create(Long optionNameId, String optionValue) {
        return new OptionValue(optionNameId, OptionValueContent.of(optionValue));
    }

    // 비즈니스 메서드
    public void updateOptionValue(String optionValue) {
        this.optionValue = OptionValueContent.of(optionValue);
    }

    public void updateOptionName(Long optionNameId) {
        this.optionNameId = optionNameId;
    }

    // 연관관계 메서드 - ProductValueOptionMapping 연결
    public void addProductValueOptionMapping(ProductValueOptionMapping mapping) {
        productValueOptionMappings.add(mapping);
    }

    public void removeProductValueOptionMapping(ProductValueOptionMapping mapping) {
        productValueOptionMappings.remove(mapping);
    }

    // JPA를 위한 protected 생성자
    protected OptionValue(Long optionNameId, OptionValueContent optionValue) {
        this.optionNameId = optionNameId;
        this.optionValue = optionValue;
    }
}