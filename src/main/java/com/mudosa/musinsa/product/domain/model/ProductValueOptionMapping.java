package com.mudosa.musinsa.product.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "product_value_option_mapping", 
       indexes = {
           @Index(name = "idx_map_prodopt", columnList = "product_option_id"),
           @Index(name = "idx_map_optval", columnList = "option_value_id")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ProductValueOptionMapping {

    @EmbeddedId
    private ProductValueOptionMappingId id;

    @MapsId("productOptionId")
    @Column(name = "product_option_id", nullable = false)
    private Long productOptionId;

    @MapsId("optionValueId")
    @Column(name = "option_value_id", nullable = false)
    private Long optionValueId;

    // 연관관계 - ProductOption 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", insertable = false, updatable = false)
    private ProductOption productOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_value_id", insertable = false, updatable = false)
    private OptionValue optionValue;

    // 생성 메서드
    public static ProductValueOptionMapping create(Long productOptionId, Long optionValueId) {
        return new ProductValueOptionMapping(productOptionId, optionValueId);
    }

    // 비즈니스 메서드
    public void updateProductOption(Long productOptionId) {
        this.productOptionId = productOptionId;
        this.id = new ProductValueOptionMappingId(productOptionId, this.optionValueId);
    }

    public void updateOptionValue(Long optionValueId) {
        this.optionValueId = optionValueId;
        this.id = new ProductValueOptionMappingId(this.productOptionId, optionValueId);
    }

    // 연관관계 메서드
    public void setProductOption(ProductOption productOption) {
        this.productOption = productOption;
    }

    public void setOptionValue(OptionValue optionValue) {
        this.optionValue = optionValue;
    }

    // JPA를 위한 protected 생성자
    protected ProductValueOptionMapping(Long productOptionId, Long optionValueId) {
        this.productOptionId = productOptionId;
        this.optionValueId = optionValueId;
        this.id = new ProductValueOptionMappingId(productOptionId, optionValueId);
    }

    // 복합 PK 클래스
    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ProductValueOptionMappingId implements java.io.Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @Column(name = "product_option_id")
        private Long productOptionId;
        
        @Column(name = "option_value_id")
        private Long optionValueId;
        
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