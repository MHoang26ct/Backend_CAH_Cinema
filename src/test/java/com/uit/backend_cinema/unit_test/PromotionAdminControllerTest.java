package com.uit.backend_cinema.unit_test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.uit.backend_cinema.modules.promotion_article.api.controller.PromotionAdminController;
import com.uit.backend_cinema.modules.promotion_article.api.dto.request.PromotionArticleRequest;
import com.uit.backend_cinema.modules.promotion_article.api.dto.response.PromotionArticlePreviewResponse;
import com.uit.backend_cinema.modules.promotion_article.api.dto.response.PromotionArticleResponse;
import com.uit.backend_cinema.modules.promotion_article.api.mapper.PromotionArticleApiMapper;
import com.uit.backend_cinema.modules.promotion_article.domain.entity.PromotionArticle;
import com.uit.backend_cinema.modules.promotion_article.domain.service.PromotionArticleService;

@WebMvcTest(controllers = PromotionAdminController.class, properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class PromotionAdminControllerTest {

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

    private ObjectMapper objectMapper;
    private PromotionArticle promotion;
    private PromotionArticlePreviewResponse previewResponse;
    private PromotionArticleResponse response;
    private PromotionArticleRequest request;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        promotion = new PromotionArticle();
        promotion.setPromotionId(1L);
        promotion.setTitle("Title");

        previewResponse = new PromotionArticlePreviewResponse();
        previewResponse.setPromotionId(1L);
        previewResponse.setTitle("Title");
        previewResponse.setShortDescription("Short");

        response = new PromotionArticleResponse();
        response.setPromotionId(1L);
        response.setTitle("Title");
        response.setShortDescription("Short");

        request = new PromotionArticleRequest();
        request.setTitle("Title");
        request.setShortDescription("Short");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(10));
        request.setConditions("Conditions");
        request.setImageUrl("Url");
        request.setNote("Note");
        request.setIsActive(true);
    }

    @Test
    void getAllPromotions_ShouldReturnPage() throws Exception {
        Page<PromotionArticle> page = new PageImpl<>(List.of(promotion));
        when(promotionService.getAllPromotionsForAdmin(any(Pageable.class))).thenReturn(page);
        when(promotionMapper.toPreviewResponse(any(PromotionArticle.class))).thenReturn(previewResponse);

        mockMvc.perform(get("/api/v1/admin/promotions")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Title"));
    }

    @Test
    void getPromotionDetail_ShouldReturnResponse() throws Exception {
        when(promotionService.getPromotionDetailForAdmin(1L)).thenReturn(promotion);
        when(promotionMapper.toResponse(promotion)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/promotions/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void createPromotion_ShouldReturnCreated() throws Exception {
        when(promotionService.createPromotion(
                anyString(), anyString(), any(LocalDate.class), any(LocalDate.class), anyString(), anyString(), anyString(), anyBoolean()
        )).thenReturn(promotion);
        when(promotionMapper.toResponse(promotion)).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void updatePromotion_ShouldReturnUpdated() throws Exception {
        when(promotionService.updatePromotion(
                eq(1L), anyString(), anyString(), any(LocalDate.class), any(LocalDate.class), anyString(), anyString(), anyString(), anyBoolean()
        )).thenReturn(promotion);
        when(promotionMapper.toResponse(promotion)).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/promotions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void deletePromotion_ShouldReturnNoContent() throws Exception {
        doNothing().when(promotionService).deletePromotion(1L);

        mockMvc.perform(delete("/api/v1/admin/promotions/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
