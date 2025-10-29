package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.OptionName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 옵션명 조회용 리포지토리.
 */
@Repository
public interface OptionNameRepository extends JpaRepository<OptionName, Long> {
}
