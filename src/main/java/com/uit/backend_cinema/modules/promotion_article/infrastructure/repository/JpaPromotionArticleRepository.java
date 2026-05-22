package com.uit.backend_cinema.modules.promotion_article.infrastructure.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.promotion_article.infrastructure.entity.PromotionArticleJpaEntity;

@Repository
public interface JpaPromotionArticleRepository extends JpaRepository<PromotionArticleJpaEntity, Long> {
    Page<PromotionArticleJpaEntity> findByIsActiveTrue(Pageable pageable);
}
