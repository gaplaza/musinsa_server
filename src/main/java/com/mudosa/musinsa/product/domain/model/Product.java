package com.mudosa.musinsa.product.domain.model;

import com.mudosa.musinsa.brand.domain.model.Brand;
import com.mudosa.musinsa.common.domain.model.BaseEntity;
import com.mudosa.musinsa.product.domain.vo.ProductGenderType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 애그리거트 루트
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product")
public class Product extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;
    
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;
    
    @Column(name = "product_info", nullable = false, columnDefinition = "TEXT")
    private String productInfo;
    
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "product_gender_type", nullable = false))
    private ProductGenderType productGenderType;
    
    // 비정규화 브랜드이름 (조회)
    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName; 
    
    @Column(name = "category_path", nullable = false, length = 255)
    private String categoryPath;
    

    
    /**
     * 상품 생성 (Builder 패턴)
     */
    @Builder
    public Product(Brand brand, String productName, String productInfo, 
                 ProductGenderType productGenderType, String brandName, String categoryPath) {
        this.brand = brand;
        this.productName = productName;
        this.productInfo = productInfo;
        this.isAvailable = true;
        this.productGenderType = productGenderType;
        this.brandName = brandName;
        this.categoryPath = categoryPath;
    }
    
    // 도메인 로직: 정보 수정
    public void modify(String productName, String productInfo, 
                    ProductGenderType productGenderType, Boolean isAvailable) {
        if (productName != null) this.productName = productName;
        if (productInfo != null) this.productInfo = productInfo;
        if (productGenderType != null) this.productGenderType = productGenderType;
        if (isAvailable != null) this.isAvailable = isAvailable;
    }
    
    // 도메인 로직: 판매 상태 변경
    public void changeAvailableStatus(Boolean isAvailable) {
        if (isAvailable != null) this.isAvailable = isAvailable;
    }
    
    // 도메인 로직: 브랜드 정보 변경
    public void changeBrand(Brand brand, String brandName) {
        if (brand != null) this.brand = brand;
        if (brandName != null) this.brandName = brandName;
    }
    
    // 도메인 로직: 카테고리 경로 변경
    public void changeCategoryPath(String categoryPath) {
        if (categoryPath != null) this.categoryPath = categoryPath;
    }
    
    // 도메인 로직: 판매 가능 여부 확인
    public boolean isAvailable() {
        return Boolean.TRUE.equals(this.isAvailable);
    }
}