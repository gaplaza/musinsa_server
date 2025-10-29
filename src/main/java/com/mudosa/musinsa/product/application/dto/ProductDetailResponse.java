package com.mudosa.musinsa.product.application.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 상품 상세 조회 응답 DTO.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductDetailResponse {

    private Long productId;
    private Long brandId;
    private String brandName;
    private String productName;
    private String productInfo;
    private String productGenderType;
    private Boolean isAvailable;
    private String categoryPath;
    private Long likeCount;
    private List<CategorySummary> categories;
    private List<ImageResponse> images;
    private List<OptionDetail> options;

    public List<CategorySummary> getCategories() {
        return categories != null ? categories : Collections.emptyList();
    }

    public List<ImageResponse> getImages() {
        return images != null ? images : Collections.emptyList();
    }

    public List<OptionDetail> getOptions() {
        return options != null ? options : Collections.emptyList();
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class CategorySummary {
        private Long categoryId;
        private String categoryName;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ImageResponse {
        private Long imageId;
        private String imageUrl;
        private Boolean isThumbnail;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class OptionDetail {
        private Long optionId;
        private BigDecimal productPrice;
        private Integer stockQuantity;
        private Boolean inventoryAvailable;
        private List<OptionValueDetail> optionValues;

        public List<OptionValueDetail> getOptionValues() {
            return optionValues != null ? optionValues : Collections.emptyList();
        }

        @Getter
        @Builder
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        @AllArgsConstructor(access = AccessLevel.PROTECTED)
        public static class OptionValueDetail {
            private Long optionValueId;
            private Long optionNameId;
            private String optionName;
            private String optionValue;
        }
    }
}
