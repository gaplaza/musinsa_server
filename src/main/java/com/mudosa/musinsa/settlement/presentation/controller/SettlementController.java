package com.mudosa.musinsa.settlement.presentation.controller;

import com.mudosa.musinsa.brand.domain.repository.BrandMemberRepository;
import com.mudosa.musinsa.common.dto.ApiResponse;
import com.mudosa.musinsa.exception.BusinessException;
import com.mudosa.musinsa.exception.ErrorCode;
import com.mudosa.musinsa.security.CustomUserDetails;
import com.mudosa.musinsa.settlement.application.SettlementQueryService;
import com.mudosa.musinsa.settlement.presentation.dto.*;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String DAILY_SORT_FIELD = "settlementDate";

    private final SettlementQueryService settlementQueryService;
    private final BrandMemberRepository brandMemberRepository;

    
    private Long validateAndGetBrandId(Long requestedBrandId, CustomUserDetails userDetails) {
        String role = userDetails.getRole();

        if ("ADMIN".equals(role)) {
            return requestedBrandId;
        }

        if ("SELLER".equals(role)) {
            Long userId = userDetails.getUserId();
            List<Long> userBrandIds = brandMemberRepository.findBrandIdsByUserId(userId);

            if (userBrandIds.isEmpty()) {
                throw new BusinessException(ErrorCode.BRAND_NOT_FOUND);
            }

            if (requestedBrandId == null) {
                return userBrandIds.getFirst();
            }

            if (!userBrandIds.contains(requestedBrandId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }

            return requestedBrandId;
        }

        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<Page<SettlementDailyResponse>>> getDailySettlements(
        @RequestParam(required = false) Long brandId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = DAILY_SORT_FIELD, direction = Sort.Direction.DESC) Pageable pageable
    ) {

        Long validatedBrandId = validateAndGetBrandId(brandId, userDetails);

        log.debug("일일 정산 목록 조회 요청 - userId: {}, role: {}, requestedBrandId: {}, validatedBrandId: {}, page: {}, size: {}",
            userDetails.getUserId(), userDetails.getRole(), brandId, validatedBrandId,
            pageable.getPageNumber(), pageable.getPageSize());

        Page<SettlementDailyResponse> data = settlementQueryService.getDailySettlements(
            validatedBrandId, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    
    @GetMapping("/daily/{settlementDailyId}")
    public ResponseEntity<ApiResponse<SettlementDailyResponse>> getDailySettlement(
        @PathVariable @Min(1) Long settlementDailyId
    ) {
        log.debug("일일 정산 상세 조회 요청 - id: {}", settlementDailyId);

        SettlementDailyResponse data = settlementQueryService.getDailySettlement(settlementDailyId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<Page<SettlementWeeklyResponse>>> getWeeklySettlements(
        @RequestParam(required = false) Long brandId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "weekStartDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        Long validatedBrandId = validateAndGetBrandId(brandId, userDetails);

        log.debug("주간 정산 목록 조회 요청 - userId: {}, role: {}, requestedBrandId: {}, validatedBrandId: {}, page: {}, size: {}",
            userDetails.getUserId(), userDetails.getRole(), brandId, validatedBrandId,
            pageable.getPageNumber(), pageable.getPageSize());

        Page<SettlementWeeklyResponse> data = settlementQueryService.getWeeklySettlements(validatedBrandId, pageable);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    
    @GetMapping("/weekly/{settlementWeeklyId}")
    public ResponseEntity<ApiResponse<SettlementWeeklyResponse>> getWeeklySettlement(
        @PathVariable @Min(1) Long settlementWeeklyId
    ) {
        log.debug("주간 정산 상세 조회 요청 - id: {}", settlementWeeklyId);

        SettlementWeeklyResponse data = settlementQueryService.getWeeklySettlement(settlementWeeklyId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<Page<SettlementMonthlyResponse>>> getMonthlySettlements(
        @RequestParam(required = false) Long brandId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "settlementYear,settlementMonth", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        Long validatedBrandId = validateAndGetBrandId(brandId, userDetails);

        log.debug("월간 정산 목록 조회 요청 - userId: {}, role: {}, requestedBrandId: {}, validatedBrandId: {}, page: {}, size: {}",
            userDetails.getUserId(), userDetails.getRole(), brandId, validatedBrandId,
            pageable.getPageNumber(), pageable.getPageSize());

        Page<SettlementMonthlyResponse> data = settlementQueryService.getMonthlySettlements(
            validatedBrandId, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    
    @GetMapping("/monthly/{settlementMonthlyId}")
    public ResponseEntity<ApiResponse<SettlementMonthlyResponse>> getMonthlySettlement(
        @PathVariable @Min(1) Long settlementMonthlyId
    ) {
        log.debug("월간 정산 상세 조회 요청 - id: {}", settlementMonthlyId);

        SettlementMonthlyResponse data = settlementQueryService.getMonthlySettlement(settlementMonthlyId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    
    @GetMapping("/yearly")
    public ResponseEntity<ApiResponse<Page<SettlementYearlyResponse>>> getYearlySettlements(
        @RequestParam(required = false) Long brandId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "settlementYear", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        Long validatedBrandId = validateAndGetBrandId(brandId, userDetails);

        log.debug("연간 정산 목록 조회 요청 - userId: {}, role: {}, requestedBrandId: {}, validatedBrandId: {}, page: {}, size: {}",
            userDetails.getUserId(), userDetails.getRole(), brandId, validatedBrandId,
            pageable.getPageNumber(), pageable.getPageSize());

        Page<SettlementYearlyResponse> data = settlementQueryService.getYearlySettlements(validatedBrandId, pageable);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    
    @GetMapping("/yearly/{settlementYearlyId}")
    public ResponseEntity<ApiResponse<SettlementYearlyResponse>> getYearlySettlement(
        @PathVariable @Min(1) Long settlementYearlyId
    ) {
        log.debug("연간 정산 상세 조회 요청 - id: {}", settlementYearlyId);

        SettlementYearlyResponse data = settlementQueryService.getYearlySettlement(settlementYearlyId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<SettlementPerTransactionResponse>>> getPerTransactionSettlements(
        @RequestParam(required = false) Long brandId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
        @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
        @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        Long validatedBrandId = validateAndGetBrandId(brandId, userDetails);

        log.debug("거래별 정산 목록 조회 요청 - userId: {}, role: {}, requestedBrandId: {}, validatedBrandId: {}, startDate: {}, endDate: {}, page: {}, size: {}",
            userDetails.getUserId(), userDetails.getRole(), brandId, validatedBrandId,
            startDate, endDate, pageable.getPageNumber(), pageable.getPageSize());

        Page<SettlementPerTransactionResponse> data = settlementQueryService.getPerTransactionSettlements(
            validatedBrandId, startDate, endDate, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<ApiResponse<SettlementPerTransactionResponse>> getPerTransactionSettlement(
        @PathVariable @Min(1) Long transactionId
    ) {
        log.debug("거래별 정산 단건 조회 요청 - id: {}", transactionId);

        SettlementPerTransactionResponse data = settlementQueryService.getPerTransactionSettlement(transactionId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<SettlementStatisticsResponse>> getStatistics(
        @RequestParam(required = false) Long brandId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        Long validatedBrandId = validateAndGetBrandId(brandId, userDetails);

        log.debug("정산 통계 조회 요청 - userId: {}, role: {}, requestedBrandId: {}, validatedBrandId: {}",
            userDetails.getUserId(), userDetails.getRole(), brandId, validatedBrandId);

        SettlementStatisticsResponse data = settlementQueryService.getStatistics(validatedBrandId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}