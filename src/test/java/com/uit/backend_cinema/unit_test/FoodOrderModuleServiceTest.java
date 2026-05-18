package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.food_order.domain.entity.Food;
import com.uit.backend_cinema.modules.food_order.domain.repository.BookingFoodDraftItemRepository;
import com.uit.backend_cinema.modules.food_order.domain.repository.FoodOrderRepository;
import com.uit.backend_cinema.modules.food_order.domain.repository.FoodRepository;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FoodOrderModuleServiceTest {

    @Test
    @DisplayName("Food order module: xóa món ăn sẽ đánh dấu deleted thay vì xóa cứng")
    void deleteMarksExistingFoodDeleted() {
        FoodRepository foodRepository = mock(FoodRepository.class);
        FoodService foodService = new FoodService(foodRepository);
        Food existing = new Food();
        existing.setFoodId(5L);
        existing.setDeleted(false);

        when(foodRepository.findById(5L)).thenReturn(Optional.of(existing));

        foodService.delete(5L);

        assertTrue(existing.isDeleted());
        verify(foodRepository).save(existing);
    }

    @Test
    @DisplayName("Food order module: calculate draft subtotal trả về 0 với danh sách null")
    void calculateDraftSubtotalReturnsZeroForNullList() {
        FoodService foodService = mock(FoodService.class);
        FoodOrderRepository foodOrderRepository = mock(FoodOrderRepository.class);
        BookingFoodDraftItemRepository draftItemRepository = mock(BookingFoodDraftItemRepository.class);
        FoodOrderService foodOrderService = new FoodOrderService(foodService, foodOrderRepository, draftItemRepository);

        assertEquals(0, foodOrderService.calculateDraftSubtotal(null).signum());
    }
}
