package com.uit.backend_cinema.unit_test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.uit.backend_cinema.modules.promotion_article.api.controller.PromotionOpenController;
import com.uit.backend_cinema.modules.promotion_article.api.dto.response.PromotionArticlePreviewResponse;
import com.uit.backend_cinema.modules.promotion_article.api.dto.response.PromotionArticleResponse;
import com.uit.backend_cinema.modules.promotion_article.api.mapper.PromotionArticleApiMapper;
import com.uit.backend_cinema.modules.promotion_article.domain.entity.PromotionArticle;
import com.uit.backend_cinema.modules.promotion_article.domain.service.PromotionArticleService;

@WebMvcTest(controllers = PromotionOpenController.class, properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class PromotionOpenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromotionArticleService promotionService;

    @MockBean
    private PromotionArticleApiMapper promotionMapper;

    @MockBean
    private com.uit.backend_cinema.common.util.JwtUtil jwtUtil;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private PromotionArticle promotion;
    private PromotionArticlePreviewResponse previewResponse;
    private PromotionArticleResponse response;

    @BeforeEach
    void setUp() {
        promotion = new PromotionArticle();
        promotion.setPromotionId(1L);
        promotion.setTitle("Title Public");

        previewResponse = new PromotionArticlePreviewResponse();
        previewResponse.setPromotionId(1L);
        previewResponse.setTitle("Title Public");
        previewResponse.setShortDescription("Short");

        response = new PromotionArticleResponse();
        response.setPromotionId(1L);
        response.setTitle("Title Public");
        response.setShortDescription("Short");
    }

    @Test
    void getPublicPromotions_ShouldReturnActivePage() throws Exception {
        Page<PromotionArticle> page = new PageImpl<>(List.of(promotion));
        when(promotionService.getAllPromotionsForPublic(any(Pageable.class))).thenReturn(page);
        when(promotionMapper.toPreviewResponse(any(PromotionArticle.class))).thenReturn(previewResponse);

        mockMvc.perform(get("/api/v1/public/promotions")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Title Public"));
    }

    @Test
    void getPublicPromotionDetail_ShouldReturnActiveResponse() throws Exception {
        when(promotionService.getPromotionDetailForPublic(1L)).thenReturn(promotion);
        when(promotionMapper.toResponse(promotion)).thenReturn(response);

        mockMvc.perform(get("/api/v1/public/promotions/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Title Public"));
    }
}
