package com.mudosa.musinsa.notification.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private String userId;
    private String title;
    private String message;
    private String linkUrl;
    private String notificationType;
    private String notificationCategory;
}
