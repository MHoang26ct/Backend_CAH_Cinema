package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.food_order.domain.entity.Category;
import com.uit.backend_cinema.modules.food_order.domain.entity.Food;
import com.uit.backend_cinema.modules.food_order.domain.repository.FoodRepository;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FoodServiceTest {
    private final FoodRepository repository = mock(FoodRepository.class);
    private final FoodService service = new FoodService(repository);

    @Test
    @DisplayName("FIND01: Trả về đủ món ăn theo ID")
    void findsAllRequestedFoods() {
        Set<Long> ids = Set.of(1L, 2L);
        Food popcorn = new Food();
        popcorn.setFoodId(1L);
        Food drink = new Food();
        drink.setFoodId(2L);
        List<Food> foods = List.of(popcorn, drink);
        when(repository.findAllByListId(ids)).thenReturn(foods);

        assertSame(foods, service.findAllByListId(ids));
        verify(repository).findAllByListId(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("FIND02: Thiếu một ID gây RESOURCE_NOT_FOUND")
    void rejectsMissingFood() {
        Set<Long> ids = Set.of(1L, 2L);
        when(repository.findAllByListId(ids)).thenReturn(List.of(new Food()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.findAllByListId(ids));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getCode());
        assertEquals("Một hoặc nhiều thức ăn/thức uống không tồn tại", exception.getMessage());
        verify(repository).findAllByListId(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("FIND03: Tập ID rỗng trả về danh sách rỗng")
    void findsNoFoodsForEmptyIds() {
        when(repository.findAllByListId(Set.of())).thenReturn(List.of());

        assertTrue(service.findAllByListId(Set.of()).isEmpty());
        verify(repository).findAllByListId(Set.of());
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UPD01: Cập nhật đủ trường và giữ nguyên ID, trạng thái deleted")
    void updatesExistingFood() {
        Food existing = new Food();
        existing.setFoodId(5L);
        existing.setName("Old name");
        existing.setDeleted(true);
        Food update = new Food();
        update.setFoodId(99L);
        update.setName("Combo mới");
        update.setDescription("Bắp và nước");
        update.setPrice(new BigDecimal("85000"));
        update.setCategory(Category.Combo);
        update.setImageUrl("https://example.com/combo.png");
        update.setAvailable(false);
        update.setDeleted(false);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Food result = service.update(5L, update);

        assertSame(existing, result);
        assertEquals(5L, result.getFoodId());
        assertEquals("Combo mới", result.getName());
        assertEquals("Bắp và nước", result.getDescription());
        assertEquals(new BigDecimal("85000"), result.getPrice());
        assertEquals(Category.Combo, result.getCategory());
        assertEquals("https://example.com/combo.png", result.getImageUrl());
        assertFalse(result.isAvailable());
        assertTrue(result.isDeleted());
        verify(repository).findById(5L);
        verify(repository).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UPD02: ID không tồn tại gây RESOURCE_NOT_FOUND và không lưu")
    void rejectsUpdateOfMissingFood() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.update(5L, new Food()));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getCode());
        assertEquals("Thức ăn/Thức uống với Id 5 không tồn tại", exception.getMessage());
        verify(repository).findById(5L);
        verify(repository, never()).save(any());
        verifyNoMoreInteractions(repository);
    }
}
