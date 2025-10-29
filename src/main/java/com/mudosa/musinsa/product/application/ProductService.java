package com.mudosa.musinsa.product.application;

import com.mudosa.musinsa.brand.domain.model.Brand;
import com.mudosa.musinsa.common.vo.Money;
import com.mudosa.musinsa.product.application.dto.ProductCreateRequest;
import com.mudosa.musinsa.product.application.dto.ProductDetailResponse;
import com.mudosa.musinsa.product.domain.model.Category;
import com.mudosa.musinsa.product.domain.model.Image;
import com.mudosa.musinsa.product.domain.model.Inventory;
import com.mudosa.musinsa.product.domain.model.OptionName;
import com.mudosa.musinsa.product.domain.model.OptionValue;
import com.mudosa.musinsa.product.domain.model.Product;
import com.mudosa.musinsa.product.domain.model.ProductLike;
import com.mudosa.musinsa.product.domain.model.ProductOption;
import com.mudosa.musinsa.product.domain.model.ProductOptionValue;
import com.mudosa.musinsa.product.domain.repository.OptionValueRepository;
import com.mudosa.musinsa.product.domain.repository.ProductLikeRepository;
import com.mudosa.musinsa.product.domain.repository.ProductRepository;
import com.mudosa.musinsa.product.domain.vo.ProductGenderType;
import com.mudosa.musinsa.product.domain.vo.StockQuantity;
import jakarta.persistence.EntityNotFoundException;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 상품 시나리오 전반을 담당하는 단일 서비스 계층.
 * DTO, 컨트롤러가 붙기 전까지 도메인 객체 위주로 동작한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final OptionValueRepository optionValueRepository;
    private final ProductLikeRepository productLikeRepository;

    /**
     * 상품/옵션/이미지/카테고리를 한 번에 생성한다.
     * 실제 구현 시에는 Brand, Category 등을 ID 기반으로 조회해 커맨드에 채운 뒤 넘겨준다.
     */
    @Transactional
    public Long createProduct(ProductCreateCommand command) {
        Product product = Product.builder()
            .brand(command.getBrand())
            .productName(command.getProductName())
            .productInfo(command.getProductInfo())
            .productGenderType(command.getProductGenderType())
            .brandName(command.getBrandName())
            .categoryPath(command.getCategoryPath())
            .isAvailable(command.getIsAvailable())
            .build();

        command.getCategories().forEach(product::addCategory);

        List<Product.ImageRegistration> imageRegistrations = command.getImages().stream()
            .map(imageSpec -> new Product.ImageRegistration(imageSpec.imageUrl(), imageSpec.isThumbnail()))
            .collect(Collectors.toList());
        product.registerImages(imageRegistrations);

        Map<Long, OptionValue> optionValueMap = loadOptionValues(command.getOptions());

        command.getOptions().forEach(optionSpec -> {
            Inventory inventory = Inventory.builder()
                .stockQuantity(new StockQuantity(optionSpec.stockQuantity()))
                .isAvailable(optionSpec.inventoryAvailable())
                .build();

            ProductOption productOption = ProductOption.builder()
                .product(product)
                .productPrice(new Money(optionSpec.productPrice()))
                .inventory(inventory)
                .build();

                List<Long> optionValueIds = optionSpec.optionValueIds() != null
                    ? optionSpec.optionValueIds()
                    : Collections.emptyList();

                optionValueIds.stream()
                    .map(id -> ProductOptionValue.builder()
                        .productOption(productOption)
                        .optionValue(optionValueMap.get(id))
                        .build())
                    .forEach(productOption::addOptionValue);

            product.addProductOption(productOption);
        });

        Product saved = productRepository.save(product);
        return saved.getProductId();
    }

    /**
     * 컨트롤러에서 DTO를 받아 도메인 커맨드로 변환하는 헬퍼.
     * Brand, Category 등은 외부 서비스/리포지토리에서 미리 조회한 뒤 주입해야 한다.
     */
    @Transactional
    public Long createProduct(ProductCreateRequest request,
                              Brand brand,
                              Category category) {
        ProductGenderType.Type genderType = ProductGenderType.Type.valueOf(request.getProductGenderType());

        validateDenormalizedFields(request, brand, category);

        List<ProductCreateCommand.ImageSpec> imageSpecs = request.getImages().stream()
            .map(image -> new ProductCreateCommand.ImageSpec(
                image.getImageUrl(),
                Boolean.TRUE.equals(image.getIsThumbnail())))
            .collect(Collectors.toList());

        List<ProductCreateCommand.OptionSpec> optionSpecs = request.getOptions().stream()
            .map(option -> new ProductCreateCommand.OptionSpec(
                option.getProductPrice(),
                option.getStockQuantity(),
                option.getInventoryAvailable(),
                option.getOptionValueIds()))
            .collect(Collectors.toList());

        ProductCreateCommand command = ProductCreateCommand.builder()
            .brand(brand)
            .productName(request.getProductName())
            .productInfo(request.getProductInfo())
            .productGenderType(new ProductGenderType(genderType))
            .brandName(request.getBrandName())
            .categoryPath(request.getCategoryPath())
            .isAvailable(request.getIsAvailable())
            .images(imageSpecs)
            .options(optionSpecs)
            .categories(category != null ? List.of(category) : Collections.emptyList())
            .build();

        return createProduct(command);
    }

    private void validateDenormalizedFields(ProductCreateRequest request,
                                            Brand brand,
                                            Category category) {
        if (!brand.getNameKo().equals(request.getBrandName())) {
            throw new IllegalArgumentException("브랜드 정보가 일치하지 않습니다. brandId=" + brand.getBrandId());
        }

        if (category == null || !category.buildPath().equals(request.getCategoryPath())) {
            throw new IllegalArgumentException("카테고리 경로가 일치하지 않습니다. categoryPath=" + request.getCategoryPath());
        }
    }

    private Map<Long, OptionValue> loadOptionValues(List<ProductCreateCommand.OptionSpec> optionSpecs) {
        Set<Long> optionValueIds = optionSpecs.stream()
                .filter(spec -> spec.optionValueIds() != null)
                .flatMap(spec -> spec.optionValueIds().stream())
            .collect(Collectors.toSet());

        if (optionValueIds.isEmpty()) {
            return Collections.emptyMap();
        }

            Map<Long, OptionValue> optionValueMap = optionValueRepository.findAllByOptionValueIdIn(new ArrayList<>(optionValueIds))
            .stream()
                .collect(Collectors.toMap(OptionValue::getOptionValueId, Function.identity()));

            if (optionValueMap.size() != optionValueIds.size()) {
                Set<Long> missingIds = new HashSet<>(optionValueIds);
                missingIds.removeAll(optionValueMap.keySet());
                throw new IllegalArgumentException("존재하지 않는 옵션 값 ID가 포함되어 있습니다: " + missingIds);
            }

            return optionValueMap;
    }

    /**
     * 상세 페이지용 조회. EntityGraph 로 주요 연관을 로딩한다.
     * 이후 DTO 설계 시 변환 단계를 추가할 예정.
     */
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findDetailById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        long likeCount = productLikeRepository.countByProduct(product);
        return mapToDetailResponse(product, likeCount);
    }

    private ProductDetailResponse mapToDetailResponse(Product product, long likeCount) {
        List<ProductDetailResponse.ImageResponse> imageResponses = product.getImages().stream()
            .map(image -> ProductDetailResponse.ImageResponse.builder()
                .imageId(image.getImageId())
                .imageUrl(image.getImageUrl())
                .isThumbnail(Boolean.TRUE.equals(image.getIsThumbnail()))
                .build())
            .collect(Collectors.toList());

        List<ProductDetailResponse.CategorySummary> categorySummaries = product.getProductCategories().stream()
            .map(mapping -> {
                Category category = mapping.getCategory();
                return ProductDetailResponse.CategorySummary.builder()
                    .categoryId(category != null ? category.getCategoryId() : null)
                    .categoryName(category != null ? category.getCategoryName() : null)
                    .build();
            })
            .collect(Collectors.toList());

        List<ProductDetailResponse.OptionDetail> optionDetails = product.getProductOptions().stream()
            .map(option -> {
                List<ProductDetailResponse.OptionDetail.OptionValueDetail> optionValueDetails = option.getProductOptionValues().stream()
                    .map(mapping -> {
                        OptionValue optionValue = mapping.getOptionValue();
                        OptionName optionName = optionValue != null ? optionValue.getOptionName() : null;
                        return ProductDetailResponse.OptionDetail.OptionValueDetail.builder()
                            .optionValueId(optionValue != null ? optionValue.getOptionValueId() : null)
                            .optionNameId(optionName != null ? optionName.getOptionNameId() : null)
                            .optionName(optionName != null ? optionName.getOptionName() : null)
                            .optionValue(optionValue != null ? optionValue.getOptionValue() : null)
                            .build();
                    })
                    .collect(Collectors.toList());

                Integer stockQuantity = null;
                Boolean inventoryAvailable = null;
                if (option.getInventory() != null && option.getInventory().getStockQuantity() != null) {
                    stockQuantity = option.getInventory().getStockQuantity().getValue();
                    inventoryAvailable = option.getInventory().getIsAvailable();
                } else if (option.getInventory() != null) {
                    inventoryAvailable = option.getInventory().getIsAvailable();
                }

                return ProductDetailResponse.OptionDetail.builder()
                    .optionId(option.getProductOptionId())
                    .productPrice(option.getProductPrice() != null ? option.getProductPrice().getAmount() : null)
                    .stockQuantity(stockQuantity)
                    .inventoryAvailable(inventoryAvailable)
                    .optionValues(optionValueDetails)
                    .build();
            })
            .collect(Collectors.toList());

        return ProductDetailResponse.builder()
            .productId(product.getProductId())
            .brandId(product.getBrand() != null ? product.getBrand().getBrandId() : null)
            .brandName(product.getBrandName())
            .productName(product.getProductName())
            .productInfo(product.getProductInfo())
            .productGenderType(product.getProductGenderType() != null && product.getProductGenderType().getValue() != null
                ? product.getProductGenderType().getValue().name()
                : null)
            .isAvailable(product.getIsAvailable())
            .categoryPath(product.getCategoryPath())
            .likeCount(likeCount)
            .categories(categorySummaries)
            .images(imageResponses)
            .options(optionDetails)
            .build();
    }

    /**
     * 검색/필터링은 QueryDSL 커스텀 구현으로 확장 예정이다.
     * 지금은 껍데기를 두고 Page.empty 로 연결만 맞춰 둔다.
     */
    public Page<Product> searchProducts(ProductSearchCondition condition) {
        Pageable pageable = condition != null ? condition.getPageable() : Pageable.unpaged();
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    /**
     * 좋아요를 토글하고 현재 카운트를 반환한다.
     * 프론트엔드에서 하드 딜리트 정책을 사용하므로 단순 토글.
     */
    @Transactional
    public long toggleLike(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        productLikeRepository.findByProductAndUserId(product, userId)
            .ifPresentOrElse(
                productLikeRepository::delete,
                () -> productLikeRepository.save(ProductLike.builder()
                    .product(product)
                    .userId(userId)
                    .build())
            );

        return productLikeRepository.countByProduct(product);
    }

    /**
     * 서비스 내부에서 사용할 상품 생성 커맨드.
     * 추후 DTO 매핑 계층이 만들어지면 외부에서는 DTO -> Command 변환만 수행한다.
     */
    @Getter
    public static class ProductCreateCommand {
        private final Brand brand;
        private final String productName;
        private final String productInfo;
        private final ProductGenderType productGenderType;
        private final String brandName;
        private final String categoryPath;
        private final Boolean isAvailable;
        private final List<ImageSpec> images;
        private final List<OptionSpec> options;
        private final List<Category> categories;

        @Builder
        public ProductCreateCommand(Brand brand,
                                    String productName,
                                    String productInfo,
                                    ProductGenderType productGenderType,
                                    String brandName,
                                    String categoryPath,
                                    Boolean isAvailable,
                                    List<ImageSpec> images,
                                    List<OptionSpec> options,
                                    List<Category> categories) {
            this.brand = brand;
            this.productName = productName;
            this.productInfo = productInfo;
            this.productGenderType = productGenderType;
            this.brandName = brandName;
            this.categoryPath = categoryPath;
            this.isAvailable = isAvailable;
            this.images = images != null ? images : Collections.emptyList();
            this.options = options != null ? options : Collections.emptyList();
            this.categories = categories != null ? categories : Collections.emptyList();
        }

        public record ImageSpec(String imageUrl, boolean isThumbnail) {}

        public record OptionSpec(BigDecimal productPrice,
                                 int stockQuantity,
                                 Boolean inventoryAvailable,
                                 List<Long> optionValueIds) {}
    }

    /**
     * 검색 조건을 보관하는 간단한 DTO.
     * 나중에 QueryDSL 구현 시 필드를 확장한다.
     */
    @Getter
    @Builder
    public static class ProductSearchCondition {
        private final String keyword;
        private final List<Long> categoryIds;
        private final ProductGenderType.Type gender;
        private final Long brandId;
        private final Pageable pageable;

        public Pageable getPageable() {
            return pageable != null ? pageable : Pageable.unpaged();
        }

        public List<Long> getCategoryIds() {
            return categoryIds != null ? categoryIds : Collections.emptyList();
        }
    }
}
