package com.mudosa.musinsa.batch.settlement.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateRangeCalculator {

    public static LocalDate getYesterday() {
        return getYesterday(LocalDate.now());
    }

    public static LocalDate getYesterday(LocalDate baseDate) {
        return baseDate.minusDays(1);
    }

    public static LocalDate getLastWeekMonday() {
        return getLastWeekMonday(LocalDate.now());
    }

    public static LocalDate getLastWeekMonday(LocalDate baseDate) {
        return baseDate.minusWeeks(1).with(DayOfWeek.MONDAY);
    }

    public static LocalDate getLastWeekSunday() {
        return getLastWeekSunday(LocalDate.now());
    }

    public static LocalDate getLastWeekSunday(LocalDate baseDate) {
        return getLastWeekMonday(baseDate).plusDays(6);
    }

    public static LocalDate getLastMonthStart() {
        return getLastMonthStart(LocalDate.now());
    }

    public static LocalDate getLastMonthStart(LocalDate baseDate) {
        YearMonth lastMonth = YearMonth.from(baseDate).minusMonths(1);
        return lastMonth.atDay(1);
    }

    public static LocalDate getLastMonthEnd() {
        return getLastMonthEnd(LocalDate.now());
    }

    public static LocalDate getLastMonthEnd(LocalDate baseDate) {
        YearMonth lastMonth = YearMonth.from(baseDate).minusMonths(1);
        return lastMonth.atEndOfMonth();
    }

    public static int getLastYear() {
        return getLastYear(LocalDate.now());
    }

    public static int getLastYear(LocalDate baseDate) {
        return baseDate.getYear() - 1;
    }
}