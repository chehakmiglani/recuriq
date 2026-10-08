package com.recuriq.billing.dto.customer;

import java.time.LocalDateTime;

import com.recuriq.billing.enums.Channel;
import com.recuriq.billing.enums.Status;

import lombok.Data;

@Data
public class NotificationDTO {
	private Long notificationId;
	private String type;
	private String subject;
	private String body;
	private Channel channel;
	private Status status;
	private LocalDateTime scheduledAt;
	private LocalDateTime sentAt;
	private LocalDateTime createdAt;
	private Boolean isRead;
}
