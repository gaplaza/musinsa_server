package com.mudosa.musinsa.common.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Slack Webhook 알림 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationService {

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    @Value("${slack.notification.enabled:false}")
    private boolean notificationEnabled;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * 정산 배치 실패 알림
     */
    @Async
    public void sendBatchFailureAlert(String batchName, Exception e) {
        if (!notificationEnabled || webhookUrl.isEmpty()) {
            log.debug("Slack 알림이 비활성화되어 있습니다.");
            return;
        }

        try {
            String message = buildBatchFailureMessage(batchName, e);
            sendMessage(message);
            log.info("Slack 알림 전송 완료 - 배치 실패: {}", batchName);
        } catch (Exception ex) {
            log.error("Slack 알림 전송 실패", ex);
        }
    }

    /**
     * Settlement 생성 실패 알림
     */
    @Async
    public void sendSettlementCreationFailure(Long paymentId, Exception e) {
        if (!notificationEnabled || webhookUrl.isEmpty()) {
            return;
        }

        try {
            String message = buildSettlementFailureMessage(paymentId, e);
            sendMessage(message);
            log.info("Slack 알림 전송 완료 - Settlement 생성 실패: PaymentId={}", paymentId);
        } catch (Exception ex) {
            log.error("Slack 알림 전송 실패", ex);
        }
    }

    /**
     * 일반 에러 알림
     */
    @Async
    public void sendErrorAlert(String title, String description, Exception e) {
        if (!notificationEnabled || webhookUrl.isEmpty()) {
            return;
        }

        try {
            String message = buildErrorMessage(title, description, e);
            sendMessage(message);
            log.info("Slack 알림 전송 완료 - {}", title);
        } catch (Exception ex) {
            log.error("Slack 알림 전송 실패", ex);
        }
    }

    private String buildBatchFailureMessage(String batchName, Exception e) {
        return String.format(
            "🚨 *정산 배치 실패*\n" +
            "• 배치명: `%s`\n" +
            "• 에러: `%s`\n" +
            "• 메시지: `%s`\n" +
            "• 시각: `%s`\n\n" +
            "⚠️ *긴급 조치 필요*",
            batchName,
            e.getClass().getSimpleName(),
            e.getMessage(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private String buildSettlementFailureMessage(Long paymentId, Exception e) {
        return String.format(
            "⚠️ *Settlement 생성 실패*\n" +
            "• PaymentId: `%d`\n" +
            "• 에러: `%s`\n" +
            "• 메시지: `%s`\n" +
            "• 시각: `%s`\n\n" +
            "💡 *Kafka 재시도 중... 3회 실패 시 복구 필요*",
            paymentId,
            e.getClass().getSimpleName(),
            e.getMessage(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private String buildErrorMessage(String title, String description, Exception e) {
        return String.format(
            "❌ *%s*\n" +
            "• 설명: `%s`\n" +
            "• 에러: `%s`\n" +
            "• 메시지: `%s`\n" +
            "• 시각: `%s`",
            title,
            description,
            e != null ? e.getClass().getSimpleName() : "N/A",
            e != null ? e.getMessage() : "N/A",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private void sendMessage(String text) throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("text", text);

        String jsonPayload = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Slack API 호출 실패: " + response.statusCode());
        }
    }
}
