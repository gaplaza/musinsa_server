package com.mudosa.musinsa.settlement.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 정산 상태
 */
@Getter
@RequiredArgsConstructor
public enum SettlementStatus {

    PENDING("대기중"),
    PROCESSING("처리중"),
    COMPLETED("정산완료"),
    FAILED("실패");

    private final String description;

}
