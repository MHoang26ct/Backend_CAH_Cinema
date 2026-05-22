package com.uit.backend_cinema.modules.promotion_article.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.promotion_article.domain.entity.PromotionArticle;
import com.uit.backend_cinema.modules.promotion_article.domain.repository.PromotionArticleRepository;
import com.uit.backend_cinema.modules.promotion_article.infrastructure.entity.PromotionArticleJpaEntity;
import com.uit.backend_cinema.modules.promotion_article.infrastructure.mapper.PromotionArticleInfraMapper;
import com.uit.backend_cinema.modules.promotion_article.infrastructure.repository.JpaPromotionArticleRepository;

@Repository
public class PromotionArticleRepositoryImpl implements PromotionArticleRepository {
    private final JpaPromotionArticleRepository jpaPromotionRepository;
    private final PromotionArticleInfraMapper mapper;

    public PromotionArticleRepositoryImpl(JpaPromotionArticleRepository jpaPromotionRepository, PromotionArticleInfraMapper mapper) {
        this.jpaPromotionRepository = jpaPromotionRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<PromotionArticle> findById(Long promotionId) {
        return jpaPromotionRepository.findById(promotionId)
                .map(mapper::toDomain);
    }

    @Override
    public PromotionArticle save(PromotionArticle article) {
        PromotionArticleJpaEntity entity = jpaPromotionRepository.save(mapper.toInfrastructure(article));
        return mapper.toDomain(entity);
    }

    @Override
    public Page<PromotionArticle> findAll(Pageable pageable) {
        return jpaPromotionRepository.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<PromotionArticle> findAllActive(Pageable pageable) {
        return jpaPromotionRepository.findByIsActiveTrue(pageable)
                .map(mapper::toDomain);
    }
}
