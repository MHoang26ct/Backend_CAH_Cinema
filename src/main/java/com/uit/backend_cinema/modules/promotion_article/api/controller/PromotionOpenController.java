package com.uit.backend_cinema.modules.promotion_article.api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.promotion_article.api.dto.response.PromotionArticlePreviewResponse;
import com.uit.backend_cinema.modules.promotion_article.api.dto.response.PromotionArticleResponse;
import com.uit.backend_cinema.modules.promotion_article.api.mapper.PromotionArticleApiMapper;
import com.uit.backend_cinema.modules.promotion_article.domain.entity.PromotionArticle;
import com.uit.backend_cinema.modules.promotion_article.domain.service.PromotionArticleService;

@RestController
@RequestMapping("/api/v1/public/promotions")
public class PromotionOpenController {
    private final PromotionArticleService promotionService;
    private final PromotionArticleApiMapper mapper;

    public PromotionOpenController(PromotionArticleService promotionService, PromotionArticleApiMapper mapper) {
        this.promotionService = promotionService;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<?> getAllActivePromotions(
            @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PromotionArticlePreviewResponse> response = promotionService.getAllPromotionsForPublic(pageable)
                .map(mapper::toPreviewResponse);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách khuyến mãi thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getActivePromotionDetail(@PathVariable Long id) {
        PromotionArticle promotion = promotionService.getPromotionDetailForPublic(id);
        PromotionArticleResponse response = mapper.toResponse(promotion);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết khuyến mãi thành công"));
    }
}
