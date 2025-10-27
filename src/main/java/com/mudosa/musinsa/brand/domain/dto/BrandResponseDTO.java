package com.mudosa.musinsa.brand.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponseDTO {
  public long brandId;
  public String nameKo;
  public String nameEn;
  public String logoURL;
}
