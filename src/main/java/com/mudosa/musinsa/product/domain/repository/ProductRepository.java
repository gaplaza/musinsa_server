package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 상품 애그리거트 접근을 담당한다.
 * 상세 조회에 필요한 연관 로딩 전략은 이후 커스텀 리포지토리에서 보강 예정이다.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
}
