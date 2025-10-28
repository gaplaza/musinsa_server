package com.mudosa.musinsa.product.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * 상품 옵션-옵션값 매핑 엔티티
 * 
 * 개선된 구조:
 * - ProductOption과 OptionValue만 직접 연결
 * - OptionName은 OptionValue를 통해 자연스럽게 접근
 * - 복합 키가 2개 컬럼으로 단순화
 * 
 * 상품 내 동일 옵션명 중복 방지:
 * - OptionValue가 이미 특정 OptionName에 종속되어 있음
 * - 따라서 ProductOption-OptionValue 조합만으로도 옵션명+값의 유니크성 보장
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_option_value", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_option_id", "option_value_id"}))
public class ProductOptionValue {
    
    @EmbeddedId
    private ProductOptionValueId id;
    
    @MapsId("productOptionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id")
    private ProductOption productOption;
    
    @MapsId("optionValueId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_value_id")
    private OptionValue optionValue;
    
    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Getter
    public static class ProductOptionValueId implements Serializable {
        
        @Column(name = "product_option_id")
        private Long productOptionId;
        
        @Column(name = "option_value_id")
        private Long optionValueId;
        
        public ProductOptionValueId(Long productOptionId, Long optionValueId) {
            this.productOptionId = productOptionId;
            this.optionValueId = optionValueId;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductOptionValueId that = (ProductOptionValueId) o;
            return Objects.equals(productOptionId, that.productOptionId) &&
                   Objects.equals(optionValueId, that.optionValueId);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(productOptionId, optionValueId);
        }
    }

    @Builder
    public ProductOptionValue(ProductOption productOption, OptionValue optionValue) {
        this.productOption = productOption;
        this.optionValue = optionValue;
        this.id = new ProductOptionValueId(
            productOption.getProductOptionId(),
            optionValue.getOptionValueId()
        );
    }
    
    // 도메인 로직: 정보 수정
    public void modify(ProductOption productOption, OptionValue optionValue) {
        if (productOption != null) {
            this.productOption = productOption;
            this.id.productOptionId = productOption.getProductOptionId();
        }
        if (optionValue != null) {
            this.optionValue = optionValue;
            this.id.optionValueId = optionValue.getOptionValueId();
        }
    }
    
    // 도메인 로직: 상품 옵션 여부 확인
    public boolean belongsToProductOption(ProductOption productOption) {
        return this.productOption != null && this.productOption.equals(productOption);
    }
    
    // 도메인 로직: 옵션 값 여부 확인
    public boolean belongsToOptionValue(OptionValue optionValue) {
        return this.optionValue != null && this.optionValue.equals(optionValue);
    }
    
    // 도메인 로직: 옵션 이름 여부 확인 (OptionValue를 통해 확인)
    public boolean belongsToOptionName(OptionName optionName) {
        return this.optionValue != null && this.optionValue.belongsTo(optionName);
    }
    
    // 도메인 로직: ProductOption 양방향 연관관계 설정
    public void assignProductOption(ProductOption productOption) {
        if (productOption != null && !productOption.equals(this.productOption)) {
            this.productOption = productOption;
            this.id.productOptionId = productOption.getProductOptionId();
        }
    }
    
    // 편의 메서드: 옵션명 얻기
    public String getOptionName() {
        return optionValue != null && optionValue.getOptionName() != null 
               ? optionValue.getOptionName().getOptionName() 
               : null;
    }
    
    // 편의 메서드: 옵션값 얻기
    public String getOptionValue() {
        return optionValue != null ? optionValue.getOptionValue() : null;
    }
    
    // 도메인 로직: 동일한 옵션명을 가진 매핑인지 확인
    public boolean hasSameOptionName(ProductOptionValue other) {
        if (other == null) return false;
        return Objects.equals(this.getOptionName(), other.getOptionName());
    }
}