package com.mudosa.musinsa.product.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * 상품옵션-옵션값 매핑 엔티티
 * 
 * 하나의 ProductOption(제품 가격 조합)은 여러 OptionValue의 조합으로 구성됩니다.
 * 예: ProductOption(150,000원) = OptionValue("270") + OptionValue("블랙")
 */
@Entity
@Table(
    name = "product_value_option_mapping",
    indexes = {
        @Index(name = "idx_map_optval", columnList = "option_value_id")
    }
)
@IdClass(ProductValueOptionMapping.ProductValueOptionMappingId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductValueOptionMapping {
    
    @Id
    @Column(name = "product_option_id")
    private Long productOptionId;
    
    @Id
    @Column(name = "option_value_id")
    private Long optionValueId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", insertable = false, updatable = false)
    private ProductOption productOption;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_value_id", insertable = false, updatable = false)
    private OptionValue optionValue;
    
    /**
     * 매핑 생성
     */
    public static ProductValueOptionMapping create(
        ProductOption productOption,
        OptionValue optionValue
    ) {
        ProductValueOptionMapping mapping = new ProductValueOptionMapping();
        mapping.productOptionId = productOption.getId();
        mapping.optionValueId = optionValue.getId();
        mapping.productOption = productOption;
        mapping.optionValue = optionValue;
        return mapping;
    }
    
    /**
     * 복합키 클래스
     */
    public static class ProductValueOptionMappingId implements Serializable {
        private Long productOptionId;
        private Long optionValueId;
        
        public ProductValueOptionMappingId() {}
        
        public ProductValueOptionMappingId(Long productOptionId, Long optionValueId) {
            this.productOptionId = productOptionId;
            this.optionValueId = optionValueId;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductValueOptionMappingId that = (ProductValueOptionMappingId) o;
            return Objects.equals(productOptionId, that.productOptionId) &&
                   Objects.equals(optionValueId, that.optionValueId);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(productOptionId, optionValueId);
        }
    }
}
