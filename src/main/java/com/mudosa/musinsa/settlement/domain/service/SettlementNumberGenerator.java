package com.mudosa.musinsa.settlement.domain.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class SettlementNumberGenerator {

    
    public String generateDailyNumber(LocalDate date) {
        String dateStr = date.toString().replace("-", "");
        String uniqueId = generateUniqueId();
        return String.format("DAILY-%s-%s", dateStr, uniqueId);
    }

    
    public String generateWeeklyNumber(int year, int week) {
        String uniqueId = generateUniqueId();
        return String.format("WEEKLY-%dW%02d-%s", year, week, uniqueId);
    }

    
    public String generateMonthlyNumber(int year, int month) {
        String uniqueId = generateUniqueId();
        return String.format("MONTHLY-%d%02d-%s", year, month, uniqueId);
    }

    
    public String generateYearlyNumber(int year) {
        String uniqueId = generateUniqueId();
        return String.format("YEARLY-%d-%s", year, uniqueId);
    }

    private String generateUniqueId() {
        return UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 8)
            .toUpperCase();
    }
}
