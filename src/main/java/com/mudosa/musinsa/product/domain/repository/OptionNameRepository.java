package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.OptionName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 옵션명 Repository
 */
@Repository
public interface OptionNameRepository extends JpaRepository<OptionName, Long> {
    
    /**
     * 옵션명으로 조회
     */
    Optional<OptionName> findByOptionName(String optionName);
    
    /**
     * 옵션명 존재 여부 확인
     */
    boolean existsByOptionName(String optionName);
}
