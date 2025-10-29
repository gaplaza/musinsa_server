package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.OptionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 옵션 값 조회용 리포지토리.
 */
@Repository
public interface OptionValueRepository extends JpaRepository<OptionValue, Long> {

    List<OptionValue> findAllByOptionValueIdIn(List<Long> optionValueIds);
}
