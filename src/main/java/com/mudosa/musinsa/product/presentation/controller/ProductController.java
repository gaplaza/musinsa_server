package com.mudosa.musinsa.product.presentation.controller;

import com.mudosa.musinsa.brand.domain.model.Brand;
import com.mudosa.musinsa.brand.domain.repository.BrandRepository;
import com.mudosa.musinsa.product.application.ProductService;
import com.mudosa.musinsa.product.application.dto.ProductCreateRequest;
import com.mudosa.musinsa.product.application.dto.ProductCreateResponse;
import com.mudosa.musinsa.product.domain.model.Category;
import com.mudosa.musinsa.product.domain.repository.CategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
/**
 * 상품 API 컨트롤러 (초안).
 * - 관리자/내부용 상품 생성
 * - 추후 사용자 상세/목록/좋아요 엔드포인트 추가 예정
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 상품 등록
     */
    @PostMapping
    public ResponseEntity<ProductCreateResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        Brand brand = brandRepository.findById(request.getBrandId())
            .orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다. brandId=" + request.getBrandId()));

        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다. categoryId=" + request.getCategoryId()));

        Long productId = productService.createProduct(request, brand, category);
        URI location = URI.create("/api/products/" + productId);
        return ResponseEntity.created(location)
            .body(ProductCreateResponse.builder().productId(productId).build());
    }
}
