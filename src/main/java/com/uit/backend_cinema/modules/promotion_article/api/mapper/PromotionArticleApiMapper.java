package com.uit.backend_cinema.modules.promotion_article.api.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.promotion_article.api.dto.response.PromotionArticlePreviewResponse;
import com.uit.backend_cinema.modules.promotion_article.api.dto.response.PromotionArticleResponse;
import com.uit.backend_cinema.modules.promotion_article.domain.entity.PromotionArticle;

@Mapper(componentModel = "spring")
public interface PromotionArticleApiMapper {
    PromotionArticleResponse toResponse(PromotionArticle domain);
    PromotionArticlePreviewResponse toPreviewResponse(PromotionArticle domain);
}
