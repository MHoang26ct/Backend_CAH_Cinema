package com.uit.backend_cinema.modules.promotion_article.domain.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.promotion_article.domain.entity.PromotionArticle;
import com.uit.backend_cinema.modules.promotion_article.domain.repository.PromotionArticleRepository;

@Service
@Transactional(readOnly = true)
public class PromotionArticleService {
    private final PromotionArticleRepository promotionRepository;

    public PromotionArticleService(PromotionArticleRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public Page<PromotionArticle> getAllPromotionsForAdmin(Pageable pageable) {
        return promotionRepository.findAll(pageable);
    }

    public Page<PromotionArticle> getAllPromotionsForPublic(Pageable pageable) {
        return promotionRepository.findAllActive(pageable);
    }

    public PromotionArticle getPromotionDetailForAdmin(Long id) {
        return promotionRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("Bài viết khuyến mãi không tồn tại hoặc đã bị xóa", ErrorCode.RESOURCE_NOT_FOUND));
    }

    public PromotionArticle getPromotionDetailForPublic(Long id) {
        return promotionRepository.findById(id)
                .filter(p -> !p.getIsDeleted() && p.getIsActive())
                .orElseThrow(() -> new BusinessException("Bài viết khuyến mãi không tồn tại hoặc không hoạt động", ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public PromotionArticle createPromotion(String title, String shortDescription, LocalDate startDate, LocalDate endDate, String conditions, String imageUrl, String note, Boolean isActive) {
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException("Tiêu đề bài viết không được để trống", ErrorCode.VALIDATION_FAILED);
        }
        if (shortDescription == null || shortDescription.trim().isEmpty()) {
            throw new BusinessException("Mô tả ngắn không được để trống", ErrorCode.VALIDATION_FAILED);
        }

        PromotionArticle promotion = new PromotionArticle();
        promotion.setTitle(title);
        promotion.setShortDescription(shortDescription);
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setConditions(conditions);
        promotion.setImageUrl(imageUrl);
        promotion.setNote(note);
        promotion.setIsActive(isActive != null ? isActive : true);

        return promotionRepository.save(promotion);
    }

    @Transactional
    public PromotionArticle updatePromotion(Long id, String title, String shortDescription, LocalDate startDate, LocalDate endDate, String conditions, String imageUrl, String note, Boolean isActive) {
        PromotionArticle promotion = promotionRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("Bài viết khuyến mãi không tồn tại hoặc đã bị xóa", ErrorCode.RESOURCE_NOT_FOUND));

        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException("Tiêu đề bài viết không được để trống", ErrorCode.VALIDATION_FAILED);
        }
        if (shortDescription == null || shortDescription.trim().isEmpty()) {
            throw new BusinessException("Mô tả ngắn không được để trống", ErrorCode.VALIDATION_FAILED);
        }

        promotion.setTitle(title);
        promotion.setShortDescription(shortDescription);
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setConditions(conditions);
        promotion.setImageUrl(imageUrl);
        promotion.setNote(note);
        
        if (isActive != null) {
            promotion.setIsActive(isActive);
        }

        return promotionRepository.save(promotion);
    }

    @Transactional
    public void deletePromotion(Long id) {
        PromotionArticle promotion = promotionRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException("Bài viết khuyến mãi không tồn tại hoặc đã bị xóa", ErrorCode.RESOURCE_NOT_FOUND));

        promotion.setIsDeleted(true);
        promotionRepository.save(promotion);
    }
}
