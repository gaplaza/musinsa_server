package com.mudosa.musinsa.settlement.batch.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 정산 기간 계산 유틸리티
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateRangeCalculator {

    /* 어제 날짜 반환 */
    public static LocalDate getYesterday() {
        return LocalDate.now().minusDays(1);
    }

    /* 지난 주 월요일 반환 */
    public static LocalDate getLastWeekMonday() {
        return LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY);
    }

    /* 지난 주 일요일 반환 */
    public static LocalDate getLastWeekSunday() {
        return getLastWeekMonday().plusDays(6);
    }

    /* 지난 달 시작일 반환 */
    public static LocalDate getLastMonthStart() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        return lastMonth.atDay(1);
    }

    /* 지난 달 종료일 반환 */
    public static LocalDate getLastMonthEnd() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        return lastMonth.atEndOfMonth();
    }

    /* 작년 연도 반환 */
    public static int getLastYear() {
        return LocalDate.now().getYear() - 1;
    }
}