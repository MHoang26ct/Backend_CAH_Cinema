package com.uit.backend_cinema.modules.food_order.domain.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.food_order.api.entity.FoodOrderItemRequestDTO;
import com.uit.backend_cinema.modules.food_order.domain.entity.BookingFoodDraftItem;
import com.uit.backend_cinema.modules.food_order.domain.entity.Food;
import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrder;
import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrderItem;
import com.uit.backend_cinema.modules.food_order.domain.repository.BookingFoodDraftItemRepository;
import com.uit.backend_cinema.modules.food_order.domain.repository.FoodOrderRepository;

@Service
public class FoodOrderService {
    private final FoodService foodService;
    private final FoodOrderRepository foodOrderRepository;
    private final BookingFoodDraftItemRepository bookingFoodDraftItemRepository;

    public FoodOrderService(FoodService foodService,
                            FoodOrderRepository foodOrderRepository,
                            BookingFoodDraftItemRepository bookingFoodDraftItemRepository) {
        this.foodService = foodService;
        this.foodOrderRepository = foodOrderRepository;
        this.bookingFoodDraftItemRepository = bookingFoodDraftItemRepository;
    }

    @Transactional
    public List<BookingFoodDraftItem> createDraftItems(Long bookingId, List<FoodOrderItemRequestDTO> foodItems) {
        List<BookingFoodDraftItem> draftItems = buildFoodDraftItems(normalizeFoodItems(foodItems));
        if (draftItems.isEmpty()) {
            return List.of();
        }
        draftItems.forEach(item -> item.setBookingId(bookingId));
        return bookingFoodDraftItemRepository.saveAll(draftItems);
    }

    public BigDecimal calculateDraftSubtotal(List<BookingFoodDraftItem> draftItems) {
        if (draftItems == null || draftItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return draftItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void finalizeDraftForBookingIfAbsent(Long bookingId) {
        if (getByBookingId(bookingId).isPresent()) {
            return;
        }

        List<BookingFoodDraftItem> draftItems = bookingFoodDraftItemRepository.findAllActiveByBookingId(bookingId);
        if (draftItems.isEmpty()) {
            return;
        }

        FoodOrder order = new FoodOrder();
        order.setBookingId(bookingId);
        List<FoodOrderItem> items = draftItems.stream()
                .map(draft -> {
                    FoodOrderItem item = new FoodOrderItem();
                    item.setFoodId(draft.getFoodId());
                    item.setQuantity(draft.getQuantity());
                    item.setPrice(draft.getUnitPrice());
                    return item;
                })
                .toList();
        order.setItems(items);
        order.setTotalPrice(calculateOrderTotalFromItems(items));
        foodOrderRepository.save(order);
    }

    @Transactional
    public void expireDraftItems(Long bookingId) {
        bookingFoodDraftItemRepository.softDeleteByBookingId(bookingId);
    }

    @Transactional
    public void purgeSoftDeletedDraftItems(LocalDateTime threshold) {
        bookingFoodDraftItemRepository.hardDeleteSoftDeletedBefore(threshold);
    }

    @Transactional(readOnly = true)
    public Optional<FoodOrder> getByBookingId(long bookingId) {
        return foodOrderRepository.getByBookingId(bookingId);
    }

    @Transactional
    public BigDecimal createFoodOrder(FoodOrder order) {
        List<FoodOrderItem> items = order.getItems();
        validateOrderItems(items);
        List<Food> foods = foodService.findAllByListId(items.stream().map(FoodOrderItem::getFoodId).collect(Collectors.toSet()));
        Map<Long, Food> foodMap = foods.stream().collect(Collectors.toMap(Food::getFoodId, f -> f));
        for (FoodOrderItem item : items) {
            Food food = foodMap.get(item.getFoodId());
            if (food == null || !food.isAvailable()) {
                throw new BusinessException("Món ăn không khả dụng", ErrorCode.VALIDATION_FAILED);
            }
            item.setPrice(food.getPrice());
        }
        order.setTotalPrice(items.stream().map(item -> item.getPrice()
                .multiply(new BigDecimal(item.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add));
        foodOrderRepository.save(order);
        return order.getTotalPrice();
    }

    private BigDecimal calculateOrderTotalFromItems(List<FoodOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<FoodOrder> getFoodOrdersByBookingIds(Set<Long> bookingIds) {
        return foodOrderRepository.getAllByListBookingId(bookingIds);
    }

    private void validateOrderItems(List<FoodOrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Danh sách món ăn không được trống", ErrorCode.VALIDATION_FAILED);
        }
        for (FoodOrderItem item : items) {
            if (item == null || item.getFoodId() == null) {
                throw new BusinessException("Món ăn không hợp lệ", ErrorCode.VALIDATION_FAILED);
            }
            if (item.getQuantity() <= 0) {
                throw new BusinessException("Số lượng món ăn phải lớn hơn 0", ErrorCode.VALIDATION_FAILED);
            }
        }
    }

    private Map<Long, Integer> normalizeFoodItems(List<FoodOrderItemRequestDTO> foodItems) {
        if (foodItems == null || foodItems.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> foodQuantityMap = new LinkedHashMap<>();
        for (FoodOrderItemRequestDTO item : foodItems) {
            if (item.getQuantity() <= 0) {
                throw new BusinessException("Số lượng món ăn phải lớn hơn 0", ErrorCode.VALIDATION_FAILED);
            }
            foodQuantityMap.merge(item.getFoodId(), item.getQuantity(), Integer::sum);
        }
        return foodQuantityMap;
    }

    private List<BookingFoodDraftItem> buildFoodDraftItems(Map<Long, Integer> foodQuantityMap) {
        if (foodQuantityMap.isEmpty()) {
            return List.of();
        }
        List<Food> foods = foodService.findAllByListId(foodQuantityMap.keySet());
        Map<Long, Food> foodMap = foods.stream().collect(Collectors.toMap(Food::getFoodId, food -> food));
        return foodQuantityMap.entrySet().stream().map(entry -> {
            Food food = foodMap.get(entry.getKey());
            if (food == null || !food.isAvailable()) {
                throw new BusinessException("Món ăn không khả dụng", ErrorCode.VALIDATION_FAILED);
            }
            BookingFoodDraftItem draftItem = new BookingFoodDraftItem();
            draftItem.setFoodId(entry.getKey());
            draftItem.setQuantity(entry.getValue());
            draftItem.setUnitPrice(food.getPrice());
            draftItem.setIsDeleted(false);
            return draftItem;
        }).toList();
    }
}
