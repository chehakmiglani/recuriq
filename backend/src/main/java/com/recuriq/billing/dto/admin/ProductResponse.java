package com.recuriq.billing.dto.admin;

import com.recuriq.billing.entity.Product;
import com.recuriq.billing.enums.Status;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long plansCount;

    public static ProductResponse from(Product product, long plansCount) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .plansCount(plansCount)
                .build();
    }
}
