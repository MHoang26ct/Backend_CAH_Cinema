package com.uit.backend_cinema.modules.promotion_article.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionArticle {
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
    private Boolean isActive = true;
    private Boolean isDeleted = false;
}
