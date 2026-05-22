package com.uit.backend_cinema.modules.promotion_article.infrastructure.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.promotion_article.domain.entity.PromotionArticle;
import com.uit.backend_cinema.modules.promotion_article.infrastructure.entity.PromotionArticleJpaEntity;

@Mapper(componentModel = "spring")
public interface PromotionArticleInfraMapper {
    PromotionArticle toDomain(PromotionArticleJpaEntity entity);
    PromotionArticleJpaEntity toInfrastructure(PromotionArticle domain);
}
