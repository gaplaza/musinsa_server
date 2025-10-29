package com.mudosa.musinsa.product;

import com.mudosa.musinsa.brand.domain.model.Brand;
import com.mudosa.musinsa.brand.domain.repository.BrandRepository;
import com.mudosa.musinsa.product.application.ProductService;
import com.mudosa.musinsa.product.application.dto.ProductCreateRequest;
import com.mudosa.musinsa.product.domain.model.Category;
import com.mudosa.musinsa.product.domain.model.Product;
import com.mudosa.musinsa.product.domain.model.ProductOption;
import com.mudosa.musinsa.product.domain.model.OptionName;
import com.mudosa.musinsa.product.domain.model.OptionValue;
import com.mudosa.musinsa.product.domain.repository.CategoryRepository;
import com.mudosa.musinsa.product.domain.repository.ProductLikeRepository;
import com.mudosa.musinsa.product.domain.vo.ProductGenderType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductCreateServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductLikeRepository productLikeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("상품 생성 요청이 애그리거트 전체를 저장한다")
    void createProduct_persistsAggregateGraph() {
        Brand brand = prepareBrand();
        Category category = prepareCategory("상의");
        OptionValue optionValue = prepareOptionValue("사이즈", "M");

        ProductCreateRequest request = ProductCreateRequest.builder()
            .brandId(brand.getBrandId())
            .productName("테스트 상품")
            .productInfo("테스트 상품 설명")
            .productGenderType(ProductGenderType.Type.ALL.name())
            .brandName(brand.getNameKo())
            .categoryPath(category.buildPath())
            .isAvailable(true)
            .categoryId(category.getCategoryId())
            .images(List.of(ProductCreateRequest.ImageCreateRequest.builder()
                .imageUrl("https://cdn.musinsa.com/product/main.jpg")
                .isThumbnail(true)
                .build()))
            .options(List.of(ProductCreateRequest.OptionCreateRequest.builder()
                .productPrice(new BigDecimal("19900"))
                .stockQuantity(10)
                .inventoryAvailable(true)
                .optionValueIds(List.of(optionValue.getOptionValueId()))
                .build()))
            .build();

        Long productId = productService.createProduct(request, brand, category);

        entityManager.flush();
        entityManager.clear();

        Product persisted = productService.getProductDetail(productId);

        assertThat(persisted.getProductName()).isEqualTo("테스트 상품");
        assertThat(persisted.getImages()).hasSize(1);
        assertThat(persisted.getImages().get(0).getIsThumbnail()).isTrue();
        assertThat(persisted.getProductOptions()).hasSize(1);
        ProductOption option = persisted.getProductOptions().get(0);
        assertThat(option.getInventory().getStockQuantity().getValue()).isEqualTo(10);
        assertThat(option.getProductOptionValues()).hasSize(1);
        assertThat(persisted.getProductCategories()).hasSize(1);
    }

    @Test
    @DisplayName("썸네일 중복 시 상품 생성이 실패한다")
    void createProduct_duplicateThumbnail_throws() {
        Brand brand = prepareBrand();
        Category category = prepareCategory("아우터");
        OptionValue optionValue = prepareOptionValue("사이즈", "L");

        ProductCreateRequest request = ProductCreateRequest.builder()
            .brandId(brand.getBrandId())
            .productName("썸네일 테스트")
            .productInfo("중복 썸네일")
            .productGenderType(ProductGenderType.Type.MEN.name())
            .brandName(brand.getNameKo())
            .categoryPath(category.buildPath())
            .isAvailable(true)
            .categoryId(category.getCategoryId())
            .images(List.of(
                ProductCreateRequest.ImageCreateRequest.builder()
                    .imageUrl("https://cdn.musinsa.com/product/1.jpg")
                    .isThumbnail(true)
                    .build(),
                ProductCreateRequest.ImageCreateRequest.builder()
                    .imageUrl("https://cdn.musinsa.com/product/2.jpg")
                    .isThumbnail(true)
                    .build()
            ))
            .options(List.of(ProductCreateRequest.OptionCreateRequest.builder()
                .productPrice(new BigDecimal("29900"))
                .stockQuantity(5)
                .inventoryAvailable(true)
                .optionValueIds(List.of(optionValue.getOptionValueId()))
                .build()))
            .build();

        assertThatThrownBy(() -> productService.createProduct(request, brand, category))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("썸네일");
    }

    @Test
    @DisplayName("썸네일이 없으면 상품 생성이 실패한다")
    void createProduct_missingThumbnail_throws() {
        Brand brand = prepareBrand();
        Category category = prepareCategory("코디");
        OptionValue optionValue = prepareOptionValue("사이즈", "S");

        ProductCreateRequest request = ProductCreateRequest.builder()
            .brandId(brand.getBrandId())
            .productName("썸네일 없음")
            .productInfo("썸네일이 빠진 경우")
            .productGenderType(ProductGenderType.Type.ALL.name())
            .brandName(brand.getNameKo())
            .categoryPath(category.buildPath())
            .isAvailable(true)
            .categoryId(category.getCategoryId())
            .images(List.of(ProductCreateRequest.ImageCreateRequest.builder()
                .imageUrl("https://cdn.musinsa.com/product/no-thumbnail.jpg")
                .isThumbnail(false)
                .build()))
            .options(List.of(ProductCreateRequest.OptionCreateRequest.builder()
                .productPrice(new BigDecimal("11900"))
                .stockQuantity(7)
                .inventoryAvailable(true)
                .optionValueIds(List.of(optionValue.getOptionValueId()))
                .build()))
            .build();

        assertThatThrownBy(() -> productService.createProduct(request, brand, category))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("썸네일은 최소 1개 이상");
    }

    @Test
    @DisplayName("존재하지 않는 상품 상세 조회 시 예외가 발생한다")
    void getProductDetail_notFound_throws() {
        assertThatThrownBy(() -> productService.getProductDetail(999L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("요청 브랜드명이 실제 브랜드와 다르면 예외가 발생한다")
    void createProduct_brandNameMismatch_throws() {
        Brand brand = prepareBrand();
        Category category = prepareCategory("셔츠");
        OptionValue optionValue = prepareOptionValue("색상", "블루");

        ProductCreateRequest request = ProductCreateRequest.builder()
            .brandId(brand.getBrandId())
            .productName("브랜드 검증")
            .productInfo("브랜드 불일치")
            .productGenderType(ProductGenderType.Type.MEN.name())
            .brandName("잘못된 브랜드")
            .categoryPath(category.buildPath())
            .isAvailable(true)
            .categoryId(category.getCategoryId())
            .images(List.of(ProductCreateRequest.ImageCreateRequest.builder()
                .imageUrl("https://cdn.musinsa.com/product/brand-mismatch.jpg")
                .isThumbnail(true)
                .build()))
            .options(List.of(ProductCreateRequest.OptionCreateRequest.builder()
                .productPrice(new BigDecimal("19900"))
                .stockQuantity(5)
                .inventoryAvailable(true)
                .optionValueIds(List.of(optionValue.getOptionValueId()))
                .build()))
            .build();

    assertThatThrownBy(() -> productService.createProduct(request, brand, category))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("브랜드 정보가 일치하지 않습니다");
    }

    @Test
    @DisplayName("요청 카테고리 경로가 실제 경로와 다르면 예외가 발생한다")
    void createProduct_categoryPathMismatch_throws() {
        Brand brand = prepareBrand();
        Category category = prepareCategory("코트");
        OptionValue optionValue = prepareOptionValue("사이즈", "95");

        ProductCreateRequest request = ProductCreateRequest.builder()
            .brandId(brand.getBrandId())
            .productName("카테고리 검증")
            .productInfo("카테고리 불일치")
            .productGenderType(ProductGenderType.Type.WOMEN.name())
            .brandName(brand.getNameKo())
            .categoryPath("잘못된/경로")
            .isAvailable(true)
            .categoryId(category.getCategoryId())
            .images(List.of(ProductCreateRequest.ImageCreateRequest.builder()
                .imageUrl("https://cdn.musinsa.com/product/category-mismatch.jpg")
                .isThumbnail(true)
                .build()))
            .options(List.of(ProductCreateRequest.OptionCreateRequest.builder()
                .productPrice(new BigDecimal("15900"))
                .stockQuantity(8)
                .inventoryAvailable(true)
                .optionValueIds(List.of(optionValue.getOptionValueId()))
                .build()))
            .build();

    assertThatThrownBy(() -> productService.createProduct(request, brand, category))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("카테고리 경로가 일치하지 않습니다");
    }

    @Test
    @DisplayName("좋아요 토글이 추가와 제거를 번갈아 수행한다")
    void toggleLike_togglesState() {
        Long productId = createSampleProduct();

        long firstToggle = productService.toggleLike(productId, 1L);
        long secondToggle = productService.toggleLike(productId, 1L);

        assertThat(firstToggle).isEqualTo(1L);
        assertThat(secondToggle).isEqualTo(0L);
        assertThat(productLikeRepository.count()).isZero();
    }

    @Test
    @DisplayName("검색 껍데기는 빈 페이지를 반환한다")
    void searchProducts_returnsEmptyPage() {
        ProductService.ProductSearchCondition condition = ProductService.ProductSearchCondition.builder()
            .keyword("니트")
            .categoryIds(Collections.singletonList(1L))
            .gender(ProductGenderType.Type.WOMEN)
            .brandId(1L)
            .pageable(PageRequest.of(0, 20))
            .build();

        Page<Product> page = productService.searchProducts(condition);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 옵션 값 ID가 포함되면 예외가 발생한다")
    void createProductCommand_unknownOptionValue_throws() {
        Brand brand = prepareBrand();
        Category category = prepareCategory("하의");

        ProductService.ProductCreateCommand command = ProductService.ProductCreateCommand.builder()
            .brand(brand)
            .productName("옵션 값 누락")
            .productInfo("옵션 값이 없는 경우")
            .productGenderType(new ProductGenderType(ProductGenderType.Type.ALL))
            .brandName(brand.getNameKo())
            .categoryPath(category.buildPath())
            .isAvailable(true)
            .categories(List.of(category))
            .images(List.of(new ProductService.ProductCreateCommand.ImageSpec("https://cdn.musinsa.com/product/main.jpg", true)))
            .options(List.of(new ProductService.ProductCreateCommand.OptionSpec(
                new BigDecimal("19900"),
                3,
                true,
                List.of(999L)
            )))
            .build();

        assertThatThrownBy(() -> productService.createProduct(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("존재하지 않는 옵션 값 ID");
    }

    private Long createSampleProduct() {
        Brand brand = prepareBrand();
        Category category = prepareCategory("패딩");
        OptionValue optionValue = prepareOptionValue("사이즈", "FREE");

        ProductCreateRequest request = ProductCreateRequest.builder()
            .brandId(brand.getBrandId())
            .productName("샘플 상품")
            .productInfo("테스트용")
            .productGenderType(ProductGenderType.Type.ALL.name())
            .brandName(brand.getNameKo())
            .categoryPath(category.buildPath())
            .isAvailable(true)
            .categoryId(category.getCategoryId())
            .images(List.of(ProductCreateRequest.ImageCreateRequest.builder()
                .imageUrl("https://cdn.musinsa.com/product/sample.jpg")
                .isThumbnail(true)
                .build()))
            .options(List.of(ProductCreateRequest.OptionCreateRequest.builder()
                .productPrice(new BigDecimal("9900"))
                .stockQuantity(20)
                .inventoryAvailable(true)
                .optionValueIds(List.of(optionValue.getOptionValueId()))
                .build()))
            .build();

    return productService.createProduct(request, brand, category);
    }

    private Brand prepareBrand() {
        return brandRepository.save(Brand.create("무도사", "MUDOSA", new BigDecimal("10.00")));
    }

    private Category prepareCategory(String name) {
        return categoryRepository.save(Category.builder()
            .categoryName(name)
            .parent(null)
            .imageUrl(null)
            .build());
    }

    private OptionValue prepareOptionValue(String optionNameLabel, String optionValueLabel) {
        OptionName optionName = OptionName.builder()
            .optionName(optionNameLabel)
            .build();
        entityManager.persist(optionName);

        OptionValue optionValue = OptionValue.builder()
            .optionName(optionName)
            .optionValue(optionValueLabel)
            .build();
        entityManager.persist(optionValue);
        entityManager.flush();

        return optionValue;
    }
}
