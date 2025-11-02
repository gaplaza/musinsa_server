package com.mudosa.musinsa.product.application.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 장바구니 목록 조회에 필요한 상품 상세 정보를 반환하는 DTO이다.
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItemDetailResponse {

    private Long cartItemId;
    private Long userId;
    private Long productId;
    private Long productOptionId;
    private String productName;
    private String productInfo;
    private String brandName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Integer stockQuantity;
    private Boolean hasStock;
    private String thumbnailUrl;
    private List<OptionValueSummary> optionValues;

    public List<OptionValueSummary> getOptionValues() {
        return optionValues != null ? optionValues : Collections.emptyList();
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class OptionValueSummary {
        private Long optionValueId;
        private String optionName;
        private String optionValue;
    }
}
