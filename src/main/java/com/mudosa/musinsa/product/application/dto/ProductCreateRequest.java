package com.mudosa.musinsa.product.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상품 등록 요청 DTO.
 * - 단일 상품 생성 플로우에서 컨트롤러 → 서비스로 전달된다.
 * - 실제 도메인 객체 변환은 어셈블러/서비스에서 수행한다 (Brand, Category, OptionValue 조회 필요).
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCreateRequest {

    @NotNull(message = "브랜드 ID는 필수입니다.")
    private Long brandId;

    @NotBlank(message = "상품명은 필수입니다.")
    private String productName;

    @NotBlank(message = "상품 정보는 필수입니다.")
    private String productInfo;

    @NotNull(message = "상품 성별 타입은 필수입니다.")
    private String productGenderType;

    @NotBlank(message = "브랜드명은 필수입니다.")
    private String brandName;

    @NotBlank(message = "카테고리 경로는 필수입니다.")
    private String categoryPath;

    private Boolean isAvailable;

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    @Size(min = 1, message = "상품 이미지는 최소 1장 이상 등록해야 합니다.")
    @Valid
    private List<ImageCreateRequest> images;

    @Size(min = 1, message = "상품 옵션은 최소 1개 이상 등록해야 합니다.")
    @Valid
    private List<OptionCreateRequest> options;

    /**
     * 상품 이미지 정보.
     */
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ImageCreateRequest {

        @NotBlank(message = "이미지 URL은 필수입니다.")
        private String imageUrl;

        private Boolean isThumbnail;
    }

    /**
     * 상품 옵션 정보 (가격, 재고, 옵션 값 연결).
     */
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class OptionCreateRequest {

        @NotNull(message = "옵션 가격은 필수입니다.")
        private BigDecimal productPrice;

        @NotNull(message = "재고 수량은 필수입니다.")
        private Integer stockQuantity;

        private Boolean inventoryAvailable;

        @NotEmpty(message = "옵션 값 ID는 최소 1개 이상이어야 합니다.")
        private List<Long> optionValueIds;
    }
}
