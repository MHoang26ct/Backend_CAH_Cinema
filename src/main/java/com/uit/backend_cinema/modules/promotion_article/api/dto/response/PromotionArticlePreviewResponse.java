package com.uit.backend_cinema.modules.promotion_article.api.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PromotionArticlePreviewResponse {
    private Long promotionId;
    private String title;
    private String shortDescription;
    private String imageUrl;
    private LocalDateTime createdAt;
    private Boolean isActive;
}
