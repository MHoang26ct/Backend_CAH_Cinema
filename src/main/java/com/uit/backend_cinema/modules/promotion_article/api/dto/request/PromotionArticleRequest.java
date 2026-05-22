package com.uit.backend_cinema.modules.promotion_article.api.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromotionArticleRequest {
    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    private String title;

    @NotBlank(message = "Mô tả ngắn không được để trống")
    private String shortDescription;

    private LocalDate startDate;
    private LocalDate endDate;
    private String conditions;
    private String imageUrl;
    private String note;
    private Boolean isActive = true;
}
