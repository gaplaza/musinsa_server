package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.OptionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 옵션값 Repository
 */
@Repository
public interface OptionValueRepository extends JpaRepository<OptionValue, Long> {
    
    /**
     * 특정 옵션명의 모든 옵션값 조회
     */
    @Query("SELECT ov FROM OptionValue ov WHERE ov.optionName.id = :optionNameId")
    List<OptionValue> findByOptionNameId(@Param("optionNameId") Long optionNameId);
    
    /**
     * 옵션명 ID와 옵션값으로 조회
     */
    @Query("SELECT ov FROM OptionValue ov " +
           "WHERE ov.optionName.id = :optionNameId " +
           "AND ov.optionValue = :optionValue")
    Optional<OptionValue> findByOptionNameIdAndValue(
        @Param("optionNameId") Long optionNameId,
        @Param("optionValue") String optionValue
    );
    
    /**
     * 특정 옵션명에 해당 옵션값이 존재하는지 확인
     */
    @Query("SELECT CASE WHEN COUNT(ov) > 0 THEN true ELSE false END " +
           "FROM OptionValue ov " +
           "WHERE ov.optionName.id = :optionNameId " +
           "AND ov.optionValue = :optionValue")
    boolean existsByOptionNameIdAndValue(
        @Param("optionNameId") Long optionNameId,
        @Param("optionValue") String optionValue
    );
}
