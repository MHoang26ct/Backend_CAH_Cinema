package com.uit.backend_cinema.modules.promotion_article.api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.modules.promotion_article.api.dto.request.PromotionArticleRequest;
import com.uit.backend_cinema.modules.promotion_article.api.dto.response.PromotionArticlePreviewResponse;
import com.uit.backend_cinema.modules.promotion_article.api.dto.response.PromotionArticleResponse;
import com.uit.backend_cinema.modules.promotion_article.api.mapper.PromotionArticleApiMapper;
import com.uit.backend_cinema.modules.promotion_article.domain.entity.PromotionArticle;
import com.uit.backend_cinema.modules.promotion_article.domain.service.PromotionArticleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/promotions")
public class PromotionAdminController {

    private final PromotionArticleService promotionService;
    private final PromotionArticleApiMapper promotionMapper;

    public PromotionAdminController(PromotionArticleService promotionService, PromotionArticleApiMapper promotionMapper) {
        this.promotionService = promotionService;
        this.promotionMapper = promotionMapper;
    }

    @GetMapping
    public ResponseEntity<Page<PromotionArticlePreviewResponse>> getAllPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PromotionArticle> promotions = promotionService.getAllPromotionsForAdmin(pageable);
        return ResponseEntity.ok(promotions.map(promotionMapper::toPreviewResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionArticleResponse> getPromotionDetail(@PathVariable Long id) {
        PromotionArticle promotion = promotionService.getPromotionDetailForAdmin(id);
        return ResponseEntity.ok(promotionMapper.toResponse(promotion));
    }

    @PostMapping
    public ResponseEntity<PromotionArticleResponse> createPromotion(@Valid @RequestBody PromotionArticleRequest request) {
        PromotionArticle promotion = promotionService.createPromotion(
                request.getTitle(),
                request.getShortDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getConditions(),
                request.getImageUrl(),
                request.getNote(),
                request.getIsActive()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionMapper.toResponse(promotion));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionArticleResponse> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionArticleRequest request) {
        PromotionArticle promotion = promotionService.updatePromotion(
                id,
                request.getTitle(),
                request.getShortDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getConditions(),
                request.getImageUrl(),
                request.getNote(),
                request.getIsActive()
        );
        return ResponseEntity.ok(promotionMapper.toResponse(promotion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }
}
