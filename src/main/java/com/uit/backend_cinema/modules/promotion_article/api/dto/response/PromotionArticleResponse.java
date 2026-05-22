package com.uit.backend_cinema.modules.promotion_article.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PromotionArticleResponse {
    private Long promotionId;
    private String title;
    private String shortDescription;
    private LocalDate startDate;
    private LocalDate endDate;
    private String conditions;
    private String imageUrl;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}
