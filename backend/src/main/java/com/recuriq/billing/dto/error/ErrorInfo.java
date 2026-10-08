package com.recuriq.billing.dto.error;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorInfo {
   private LocalDateTime timestamp;
   private int status;
   private String error;
   private String path;
   private String message;
}
