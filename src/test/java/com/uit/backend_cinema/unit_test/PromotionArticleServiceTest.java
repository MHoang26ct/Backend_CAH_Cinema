package com.uit.backend_cinema.unit_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.modules.promotion_article.domain.entity.PromotionArticle;
import com.uit.backend_cinema.modules.promotion_article.domain.repository.PromotionArticleRepository;
import com.uit.backend_cinema.modules.promotion_article.domain.service.PromotionArticleService;

@ExtendWith(MockitoExtension.class)
public class PromotionArticleServiceTest {

    @Mock
    private PromotionArticleRepository promotionRepository;

    @InjectMocks
    private PromotionArticleService promotionService;

    private PromotionArticle promotion;
    private Pageable pageable;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.now();
        endDate = LocalDate.now().plusDays(30);

        promotion = new PromotionArticle();
        promotion.setPromotionId(1L);
        promotion.setTitle("Test Title");
        promotion.setShortDescription("Test Short Description");
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setConditions("Test conditions");
        promotion.setImageUrl("http://image.url");
        promotion.setNote("Test note");
        promotion.setIsActive(true);
        promotion.setIsDeleted(false);

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getAllPromotionsForAdmin_ShouldReturnPage() {
        Page<PromotionArticle> page = new PageImpl<>(List.of(promotion));
        when(promotionRepository.findAll(pageable)).thenReturn(page);

        Page<PromotionArticle> result = promotionService.getAllPromotionsForAdmin(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(promotionRepository).findAll(pageable);
    }

    @Test
    void getAllPromotionsForPublic_ShouldReturnActivePage() {
        Page<PromotionArticle> page = new PageImpl<>(List.of(promotion));
        when(promotionRepository.findAllActive(pageable)).thenReturn(page);

        Page<PromotionArticle> result = promotionService.getAllPromotionsForPublic(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(promotionRepository).findAllActive(pageable);
    }

    @Test
    void getPromotionDetailForAdmin_WhenExists_ShouldReturnPromotion() {
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion));

        PromotionArticle result = promotionService.getPromotionDetailForAdmin(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPromotionId());
    }

    @Test
    void getPromotionDetailForAdmin_WhenDeleted_ShouldThrowException() {
        promotion.setIsDeleted(true);
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion));

        assertThrows(BusinessException.class, () -> promotionService.getPromotionDetailForAdmin(1L));
    }

    @Test
    void getPromotionDetailForPublic_WhenActive_ShouldReturnPromotion() {
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion));

        PromotionArticle result = promotionService.getPromotionDetailForPublic(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPromotionId());
    }

    @Test
    void getPromotionDetailForPublic_WhenInactive_ShouldThrowException() {
        promotion.setIsActive(false);
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion));

        assertThrows(BusinessException.class, () -> promotionService.getPromotionDetailForPublic(1L));
    }

    @Test
    void createPromotion_WithValidData_ShouldReturnCreated() {
        when(promotionRepository.save(any(PromotionArticle.class))).thenReturn(promotion);

        PromotionArticle result = promotionService.createPromotion("Test Title", "Test Short Description", startDate, endDate, "Cond", "URL", "Note", true);

        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
        verify(promotionRepository).save(any(PromotionArticle.class));
    }

    @Test
    void createPromotion_WithEmptyTitle_ShouldThrowException() {
        assertThrows(BusinessException.class, () ->
                promotionService.createPromotion("", "Short", startDate, endDate, "Cond", "URL", "Note", true));
    }

    @Test
    void createPromotion_WithEmptyDescription_ShouldThrowException() {
        assertThrows(BusinessException.class, () ->
                promotionService.createPromotion("Title", "", startDate, endDate, "Cond", "URL", "Note", true));
    }

    @Test
    void updatePromotion_WithValidData_ShouldReturnUpdated() {
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion));
        when(promotionRepository.save(any(PromotionArticle.class))).thenReturn(promotion);

        PromotionArticle result = promotionService.updatePromotion(1L, "New Title", "New Desc", startDate, endDate, "Cond", "URL", "Note", false);

        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        verify(promotionRepository).save(any(PromotionArticle.class));
    }

    @Test
    void deletePromotion_WhenExists_ShouldSoftDelete() {
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion));
        when(promotionRepository.save(any(PromotionArticle.class))).thenReturn(promotion);

        promotionService.deletePromotion(1L);

        assertTrue(promotion.getIsDeleted());
        verify(promotionRepository).save(promotion);
    }
}
