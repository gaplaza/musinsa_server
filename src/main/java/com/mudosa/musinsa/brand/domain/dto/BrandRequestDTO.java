package com.mudosa.musinsa.brand.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandRequestDTO {
  public String nameKo;
  public String nameEn;
  public BigDecimal commissionRate;
}
