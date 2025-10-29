package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.Product;
import com.mudosa.musinsa.product.domain.model.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 상품 옵션 개별 접근용 리포지토리.
 * 상품 수정 시 옵션만 별도로 다뤄야 할 때 활용한다.
 */
@Repository
public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

    List<ProductOption> findAllByProduct(Product product);
}
