package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 상품 상세 조회 전용 커스텀 구현.
 *
 * <p>단일 쿼리로 모든 컬렉션을 fetch join 하면 Hibernate가 다중 Bag 로딩 예외를 던지므로,
 * 브랜드만 즉시 로딩하고 나머지 연관은 순차적으로 초기화한다.</p>
 */
@Repository
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Product> findDetailById(Long productId) {
        TypedQuery<Product> query = entityManager.createQuery(
            "select p from Product p " +
                "join fetch p.brand " +
                "where p.productId = :productId",
            Product.class
        );
        query.setParameter("productId", productId);

        List<Product> results = query.getResultList();
        if (results.isEmpty()) {
            return Optional.empty();
        }

        Product product = results.get(0);
        initializeCollections(product);
        return Optional.of(product);
    }

    /**
     * 상세 화면에서 필요한 연관을 순차적으로 초기화한다.
     */
    private void initializeCollections(Product product) {
        Hibernate.initialize(product.getImages());
        Hibernate.initialize(product.getProductCategories());
        Hibernate.initialize(product.getProductOptions());

        product.getProductOptions().forEach(option -> {
            Hibernate.initialize(option.getInventory());
            Hibernate.initialize(option.getProductOptionValues());
            option.getProductOptionValues().forEach(mapping -> {
                if (mapping != null) {
                    Hibernate.initialize(mapping.getOptionValue());
                    if (mapping.getOptionValue() != null) {
                        Hibernate.initialize(mapping.getOptionValue().getOptionName());
                    }
                }
            });
        });
    }
}
