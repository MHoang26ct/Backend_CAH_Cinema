package com.uit.backend_cinema.modules.promotion_article.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.uit.backend_cinema.modules.promotion_article.domain.entity.PromotionArticle;

public interface PromotionArticleRepository {
    Optional<PromotionArticle> findById(Long promotionId);
    PromotionArticle save(PromotionArticle article);
    Page<PromotionArticle> findAll(Pageable pageable);
    Page<PromotionArticle> findAllActive(Pageable pageable);
}
