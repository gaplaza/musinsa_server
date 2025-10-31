package com.mudosa.musinsa.product.domain.repository;

import com.mudosa.musinsa.product.domain.model.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

    @Query("SELECT DISTINCT po FROM ProductOption po " +
           "JOIN FETCH po.inventory " +
           "WHERE po.productOptionId IN :ids")
    List<ProductOption> findAllByIdWithInventory(@Param("ids") List<Long> ids);
}
